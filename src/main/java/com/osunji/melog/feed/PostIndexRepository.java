package com.osunji.melog.feed;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import com.osunji.melog.elk.entity.PostIndex;

public interface PostIndexRepository extends ElasticsearchRepository<PostIndex, String> {}