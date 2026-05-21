package com.qaassist.domain.artifact;

import com.qaassist.domain.requirement.UserStory;
import com.qaassist.domain.requirement.AcceptanceCriterion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TraceabilityBuilder {

  public Traceability build(UserStory story, AcceptanceCriterion ac, TestCase testCase) {
    return new Traceability(
        UUID.randomUUID(),
        List.of(story.source()),
        List.of(ac.title()),
        story.title(),
        "jira:" + story.source()
    );
  }
}