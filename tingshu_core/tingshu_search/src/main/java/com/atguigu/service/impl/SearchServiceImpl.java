package com.atguigu.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.AlbumFeignClient;
import com.atguigu.CategoryFeignClient;
import com.atguigu.SearchFeignClient;
import com.atguigu.UserFeignClient;
import com.atguigu.entity.*;
import com.atguigu.query.AlbumIndexQuery;
import com.atguigu.repository.AlbumRepository;
import com.atguigu.service.SearchService;
import com.atguigu.vo.AlbumInfoIndexVo;
import com.atguigu.vo.AlbumSearchResponseVo;
import com.atguigu.vo.UserInfoVo;
import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private SearchFeignClient searchFeignClient;
    @Autowired
    private CategoryFeignClient categoryFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Override
    public void onSaleAlbum(Long albumId) {
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        //根据albumId查询albumInfo信息 已写过，调用feign todo 判断albumInfo是否为空
        AlbumInfo albumInfo = albumFeignClient.getAlbumInfoById(albumId).getData();
        //对拷属性
        BeanUtils.copyProperties(albumInfo, albumInfoIndex);
        //根据albumId查询专辑属性值
        List<AlbumAttributeValue> albumPropertyValueList = albumFeignClient.getAlbumPropertyValue(albumId);
        if (!CollectionUtils.isEmpty(albumPropertyValueList)) {//专辑属性值不为空
            List<AttributeValueIndex> valueIndexList = albumPropertyValueList.stream().map(albumPropertyValue -> {
                AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                BeanUtils.copyProperties(albumPropertyValue, attributeValueIndex);
                return attributeValueIndex;
            }).collect(Collectors.toList());
            albumInfoIndex.setAttributeValueIndexList(valueIndexList);
        }
        //根据三级分类id查询专辑的分类信息  todo 判断albumInfo是否为空
        BaseCategoryView categoryView = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
        albumInfoIndex.setCategory1Id(categoryView.getCategory1Id());
        albumInfoIndex.setCategory2Id(categoryView.getCategory2Id());
        //根据用户id查询用户信息
        UserInfoVo userInfo = userFeignClient.getUserById(albumInfo.getUserId()).getData();
        albumInfoIndex.setAnnouncerName(userInfo.getNickname());
        //还需要查询统计表 不查用模拟数据
        int num1 = new Random().nextInt(1000);
        int num2 = new Random().nextInt(100);
        int num3 = new Random().nextInt(50);
        int num4 = new Random().nextInt(300);
        albumInfoIndex.setPlayStatNum(num1);
        albumInfoIndex.setSubscribeStatNum(num2);
        albumInfoIndex.setBuyStatNum(num3);
        albumInfoIndex.setCommentStatNum(num4);
        //计算公式：未实现
        double hotScore = num1 * 0.2 + num2 * 0.3 + num3 * 0.4 + num4 * 0.1;
        albumInfoIndex.setHotScore(hotScore);
        albumRepository.save(albumInfoIndex);
    }

    @Override
    public void offSaleAlbum(Long albumId) {
        albumRepository.deleteById(albumId);
    }


    @SneakyThrows
    @Override
    public List<Map<Object, Object>> getChannelData(Long category1Id) {
        //1.获取三级分类信息
        List<BaseCategory3> category3List = categoryFeignClient.getCategory3ListByCategory1Id(category1Id).getData();
        //2.把三级分类id转换为List<FieldValue>
        List<FieldValue> category3FieldValueList = category3List.stream().map(BaseCategory3::getId)
                //FieldValue.of的作用是把id包装成_kind和_value类型
                .map(x -> FieldValue.of(x)).collect(Collectors.toList());
        //3.搜索ES语句 传递一个searchRequest
        SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(s -> s
                        .index("albuminfo")
                        .query(q -> q
                                .terms(t -> t
                                        .field("category3Id")
                                        .terms(new TermsQueryField.Builder().value(category3FieldValueList).build())
                                )
                        )
                        .aggregations("category3IdAgg", a -> a.terms(t -> t.field("category3Id"))
                                .aggregations("topSixHotScoreAgg", xa -> xa.topHits(t -> t.size(6)
                                        .sort(xs -> xs.field(f -> f.field("hotScore").order(SortOrder.Desc)))))),
                AlbumInfoIndex.class);
        //4.建立三级分类id和三级分类对象的映射
        Map<Long, BaseCategory3> category3Map = category3List.stream().collect(Collectors
                .toMap(BaseCategory3::getId, baseCategory3 -> baseCategory3));
        //4.解析数据放到对象当中
        Aggregate category3IdAgg = response.aggregations().get("category3IdAgg");
        List<Map<Object, Object>> topAlbumInfoIndexMapList = category3IdAgg.lterms().buckets().array().stream().map(bucket -> {
            Long category3Id = bucket.key();
            Aggregate topSixHotScoreAgg = bucket.aggregations().get("topSixHotScoreAgg");
            List<AlbumInfoIndex> topAlbumInfoIndexList = topSixHotScoreAgg.topHits().hits().hits().stream().map(hit ->
                    JSONObject.parseObject(hit.source().toString(), AlbumInfoIndex.class)
            ).collect(Collectors.toList());
            Map<Object, Object> retMap = new HashMap<>();
            retMap.put("baseCategory3", category3Map.get(category3Id));
            retMap.put("list", topAlbumInfoIndexList);
            return retMap;
        }).collect(Collectors.toList());
        return topAlbumInfoIndexMapList;
    }

    @SneakyThrows
    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {
        //1.生成DSL语句
        SearchRequest request = buildQueryDsl(albumIndexQuery);
        //2.实现对DSL语句的调用
        SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(request, AlbumInfoIndex.class);
        //3.对结果进行解析
        AlbumSearchResponseVo responseVo = parseSearchResult(response);
        //4.设置其他参数
        responseVo.setPageNo(albumIndexQuery.getPageNo());//传给前端
        responseVo.setPageSize(albumIndexQuery.getPageSize());
        //5.设置总页数
        long totalPages = (responseVo.getTotal() + responseVo.getPageSize() - 1) / responseVo.getPageSize();
        responseVo.setTotalPages(totalPages);
        return responseVo;
    }

    private AlbumSearchResponseVo parseSearchResult(SearchResponse<AlbumInfoIndex> response) {
        AlbumSearchResponseVo responseVo = new AlbumSearchResponseVo();
        //获取总记录数
        responseVo.setTotal(response.hits().total().value());
        //获取专辑列表信息
        List<Hit<AlbumInfoIndex>> searchAlbumInfoHits = response.hits().hits();
        List<AlbumInfoIndexVo> albumInfoIndexVoList = new ArrayList<>();
        for (Hit<AlbumInfoIndex> searchAlbumInfoHit : searchAlbumInfoHits) {
            AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
            BeanUtils.copyProperties(searchAlbumInfoHit.source(), albumInfoIndexVo);
            //设置高亮
            List<String> albumTitleHightList = searchAlbumInfoHit.highlight().get("albumTitle");
            if (albumTitleHightList != null) {//如果存在高亮
                albumInfoIndexVo.setAlbumTitle(albumTitleHightList.get(0));
            }
            albumInfoIndexVoList.add(albumInfoIndexVo);
        }
        responseVo.setList(albumInfoIndexVoList);
        return responseVo;
    }

    private SearchRequest buildQueryDsl(AlbumIndexQuery albumIndexQuery) {
        //2.构造一个bool
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        String keyword = albumIndexQuery.getKeyword();
        //3.构造should关键字查询
        if (!StringUtils.isEmpty(keyword)) {
            boolQuery.should(s -> s.match(m -> m.field("albumTitle").query(keyword)));
            boolQuery.should(s -> s.match(m -> m.field("albumIntro").query(keyword)));
            boolQuery.should(s -> s.match(m -> m.field("announcerName").query(keyword)));
        }
        //4.根据一级分类查询
        Long category1Id = albumIndexQuery.getCategory1Id();
        if (category1Id != null) {
            boolQuery.filter(f -> f.term(t -> t.field("category1Id").value(category1Id)));
        }
        //4.根据二级分类查询
        Long category2Id = albumIndexQuery.getCategory2Id();
        if (category2Id != null) {
            boolQuery.filter(f -> f.term(t -> t.field("category2Id").value(category2Id)));
        }
        //4.根据三级分类查询
        Long category3Id = albumIndexQuery.getCategory3Id();
        if (category3Id != null) {
            boolQuery.filter(f -> f.term(t -> t.field("category3Id").value(category3Id)));
        }
        //5.根据分类属性嵌套过滤
        List<String> propertyList = albumIndexQuery.getAttributeList();
        if (!CollectionUtils.isEmpty(propertyList)) {
            for (String property : propertyList) {
                //property数据类似于这种格式-->18:38
                String[] propertySplit = property.split(":");
                if (propertySplit != null && propertySplit.length == 2) {
                    Query nestedQuery = NestedQuery.of(f -> f.path("attributeValueIndexList")
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.term(t -> t.field("attributeValueIndexList.attributeId").value(propertySplit[0])))
                                    .must(m -> m.term(t -> t.field("attributeValueIndexList.valueId").value(propertySplit[1])))
                            )))._toQuery();
                    boolQuery.filter(nestedQuery);
                }
            }
        }
        //1.构建最外层的query
        Query query = boolQuery.build()._toQuery();
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        //6.构造分页与高亮
        SearchRequest.Builder searchRequest = new SearchRequest.Builder()
                .index("albuminfo")
                .query(query)
                .from((pageNo - 1) * pageSize)
                .size(pageSize)
                .highlight(h -> h.fields("albumTitle", p -> p
                        .preTags("<font color='red'>")
                        .postTags("</font>")));
        //7.构造排序 order=1:asc
        String order = albumIndexQuery.getOrder();
        String orderFiled = "";
        if (!StringUtils.isEmpty(order)) {
            String[] orderSplit = order.split(":");
            if (orderSplit != null && orderSplit.length == 2) {
                switch (orderSplit[0]) {
                    case "1":
                        orderFiled = "hotScore";
                        break;
                    case "2":
                        orderFiled = "playStatNum";
                        break;
                    case "3":
                        orderFiled = "createTime";
                        break;
                }
            }
            String sortType = orderSplit[1];
            String finalOrderFiled = orderFiled;
            searchRequest.sort(s -> s.field(f -> f.field(finalOrderFiled)
                    .order("asc".equals(sortType) ? SortOrder.Asc : SortOrder.Desc)));
        }
        SearchRequest request = searchRequest.build();
        System.out.println("拼接的DSL语句:" + request.toString());
        return request;
    }
}

