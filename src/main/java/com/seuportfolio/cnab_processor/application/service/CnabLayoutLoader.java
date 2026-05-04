package com.seuportfolio.cnab_processor.application.service;

import com.seuportfolio.cnab_processor.domain.model.CnabLayout;
import com.seuportfolio.cnab_processor.domain.model.FieldRange;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Carrega e faz cache dos layouts CNAB a partir dos arquivos YAML
 * em {@code src/main/resources/layouts/}.
 *
 * <p>Estratégia de resolução:</p>
 * <ol>
 *   <li>Procura layout específico por formato e banco: {@code cnab240-001.yml}</li>
 *   <li>Faz merge com o layout default do formato: {@code cnab240-default.yml}</li>
 *   <li>Campos específicos sobrescrevem os defaults (banco pode personalizar posições)</li>
 * </ol>
 */
@Slf4j
@Component
public class CnabLayoutLoader {

    /** cache: "cnab240-001" → CnabLayout */
    private final Map<String, CnabLayout> cache = new HashMap<>();

    @PostConstruct
    public void loadAll() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:layouts/*.yml");

        for (Resource resource : resources) {
            String name = resource.getFilename(); // ex: cnab240-001.yml
            if (name == null) continue;
            String key = name.replace(".yml", ""); // cnab240-001
            CnabLayout layout = parseYaml(resource.getInputStream(), key);
            cache.put(key, layout);
            log.info("Layout carregado: '{}'", key);
        }

        // Merge: layouts específicos por banco herdam do default
        mergeWithDefaults("cnab240");
        mergeWithDefaults("cnab400");

        log.info("CnabLayoutLoader inicializado — {} layouts no cache: {}",
                cache.size(), cache.keySet());
    }

    /**
     * Retorna o layout para um formato/banco.
     * Fallback: default do formato.
     */
    public CnabLayout getLayout(String format, String bankCode) {
        String specificKey = format + "-" + bankCode;
        String defaultKey  = format + "-default";

        return Optional.ofNullable(cache.get(specificKey))
                .orElseGet(() -> {
                    log.debug("Layout específico '{}' não encontrado — usando '{}'", specificKey, defaultKey);
                    return cache.get(defaultKey);
                });
    }

    private void mergeWithDefaults(String format) {
        CnabLayout defaultLayout = cache.get(format + "-default");
        if (defaultLayout == null) return;

        cache.forEach((key, layout) -> {
            if (key.startsWith(format + "-") && !key.endsWith("-default")) {
                Map<String, Map<String, FieldRange>> merged = new HashMap<>(defaultLayout.segments());
                layout.segments().forEach((seg, fields) ->
                        merged.merge(seg, fields, (defaultFields, overrideFields) -> {
                            Map<String, FieldRange> m = new HashMap<>(defaultFields);
                            m.putAll(overrideFields); // campos específicos sobrescrevem
                            return m;
                        })
                );
                cache.put(key, new CnabLayout(layout.bankCode(), layout.version(), merged));
                log.debug("Layout '{}' mesclado com default", key);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private CnabLayout parseYaml(InputStream is, String key) {
        Yaml yaml = new Yaml();
        Map<String, Object> raw = yaml.load(is);

        String bankCode = (String) raw.getOrDefault("bankCode", "default");
        String version  = (String) raw.getOrDefault("version", "1.0");

        Map<String, Map<String, FieldRange>> segments = new HashMap<>();
        Map<String, Object> rawSegments = (Map<String, Object>) raw.getOrDefault("segments", Map.of());

        rawSegments.forEach((segName, segValue) -> {
            Map<String, Object> segMap = (Map<String, Object>) segValue;
            Map<String, Object> fields = (Map<String, Object>) segMap.get("fields");
            Map<String, FieldRange> fieldRanges = new HashMap<>();

            fields.forEach((fieldName, fieldValue) -> {
                Map<String, Integer> range = (Map<String, Integer>) fieldValue;
                fieldRanges.put(fieldName, new FieldRange(range.get("start"), range.get("end")));
            });

            segments.put(segName, fieldRanges);
        });

        return new CnabLayout(bankCode, version, segments);
    }
}
