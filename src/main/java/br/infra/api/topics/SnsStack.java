package br.infra.api.topics;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;
import software.constructs.Construct;

public class SnsStack extends Stack {
    private final SnsTopic productEventTopic;

    public SnsStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public SnsStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        productEventTopic = SnsTopic.Builder.create(Topic.Builder.create(this, "productEventTopic")
                .topicName("product-event")
                .build()).build();

        productEventTopic.getTopic().addSubscription(EmailSubscription
                .Builder.create("").json(true).build());

    }

    public SnsTopic getProductEventTopic() {
        return productEventTopic;
    }
}
