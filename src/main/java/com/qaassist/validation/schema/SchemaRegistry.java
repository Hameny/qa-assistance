package com.qaassist.validation.schema;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SchemaRegistry {

  private final Map<String, JsonSchema> schemas = new ConcurrentHashMap<>();
  private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

  @PostConstruct
  public void loadSchemas() throws IOException {
    var resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath:schemas/*.json");

    for (Resource res : resources) {
      String id = res.getFilename().replace(".json", "");
      schemas.put(id, factory.getSchema(res.getInputStream()));
      log.info("✅ Loaded JSON Schema: {}", id);
    }
  }

  public JsonSchema getSchema(String id) {
    return schemas.get(id);
  }

  public boolean hasSchema(String id) {
    return schemas.containsKey(id);
  }
}