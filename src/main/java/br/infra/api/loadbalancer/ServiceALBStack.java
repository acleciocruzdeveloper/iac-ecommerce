package br.infra.api.loadbalancer;

import br.infra.api.EParameters;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

import static br.infra.api.EParameters.*;

public class ServiceALBStack extends Stack {

    public ServiceALBStack(final Construct scope, final String id, final Cluster cluster, SnsTopic productTopic) {
        this(scope, id, null, cluster, productTopic);
    }

    public ServiceALBStack(final Construct scope, final String id, final StackProps props, Cluster cluster, SnsTopic productTopic) {
        super(scope, id, props);

        Map<String, String> environments = new HashMap<>();
        environments.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://" + Fn.importValue("databaseEndPoint")
                + ":5432/ecommercedb?createDatabaseIfNotExist=true");
        environments.put("SPRING_DATASOURCE_USERNAME", "postgres");
        environments.put("SPRING_DATASOURCE_PASSWORD", Fn.importValue("dbPassword"));
        environments.put("AWS_REGION", "us-east-1");
        environments.put("SNS_TOPIC_PRODUCT_EVENTS_ARN", productTopic.getTopic().getTopicArn());

        ApplicationLoadBalancedFargateService fargateService = ApplicationLoadBalancedFargateService
                .Builder.create(this, EParameters.ECOMMERCE_SERVICE.getValue())
                .serviceName(EParameters.ECOMMERCE_SERVICE.getValue())
                .cluster(cluster)
                .cpu(512)
                .desiredCount(2)
                .listenerPort(8080)
                .memoryLimitMiB(1024)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .containerName("e-commerce-api")
                                .image(ContainerImage.fromRegistry(DOCKER_API_PRODUCTS.getValue()+":1.0.0"))
                                .containerPort(8080)
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                        .logGroup(LogGroup.Builder.create(this, "ecommerce-service-log-group")
                                                .logGroupName(EParameters.ECOMMERCE_SERVICE.getValue())
                                                .removalPolicy(RemovalPolicy.DESTROY)
                                                .build())
                                        .streamPrefix(EParameters.ECOMMERCE_SERVICE.getValue())
                                        .build()))
                                .environment(environments)
                                .build()
                )
                .publicLoadBalancer(true)
                .build();

        fargateService.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
                .path("/actuator/health")
                .port("8080")
                .healthyHttpCodes("200")
                .build());

        ScalableTaskCount scalableTaskCount = fargateService.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(2)
                .maxCapacity(4)
                .build());

        scalableTaskCount.scaleOnCpuUtilization("service-autoscaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(50)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
                .build());

        productTopic.getTopic().grantPublish(fargateService.getTaskDefinition().getTaskRole());

    }

}
