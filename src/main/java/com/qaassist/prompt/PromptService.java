package com.qaassist.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

  private final PromptRegistry registry;
  private final PromptRenderer renderer;
  private final PromptValidator validator;

  /**
   * Полный цикл: загрузка шаблона → рендеринг → валидация → готовый промпт.
   */
  public RenderedPrompt preparePrompt(String templateId, Map<String, Object> variables) {
    PromptTemplate template = registry.getTemplate(templateId);

    String renderedSystem = template.systemPrompt(); // Обычно не содержит переменных
    String renderedUser = renderer.render(template.userTemplate(), variables);

    validator.validate(template, variables, renderedSystem + renderedUser);

    return new RenderedPrompt(
        template.id(),
        template.version(),
        renderedSystem,
        renderedUser,
        template.maxTokens()
    );
  }

  /**
   * Получить шаблон без рендеринга (для UI, отладки, dry-run).
   */
  public PromptTemplate getTemplate(String templateId) {
    return registry.getTemplate(templateId);
  }

  /**
   * A/B-тестирование: принудительно выбрать версию.
   */
  public RenderedPrompt preparePromptVersion(String templateId, String version, Map<String, Object> variables) {
    PromptTemplate template = registry.getTemplateVersion(templateId, version);
    String renderedUser = renderer.render(template.userTemplate(), variables);
    validator.validate(template, variables, template.systemPrompt() + renderedUser);

    return new RenderedPrompt(
        template.id(), version, template.systemPrompt(), renderedUser, template.maxTokens()
    );
  }
}