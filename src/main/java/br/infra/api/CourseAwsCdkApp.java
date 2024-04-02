package br.infra.api;

import br.infra.api.databases.RdsStack;
import br.infra.api.ecs.ClusterStack;
import br.infra.api.loadbalancer.ServiceALBStack;
import br.infra.api.topics.SnsStack;
import br.infra.api.vpcs.VpcStack;
import software.amazon.awscdk.App;

public class CourseAwsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        VpcStack vpc = new VpcStack(app, "vpc-cluster-ecommerce");

        ClusterStack clusterStack = new ClusterStack(app, "cluster-ecommerce", vpc.getVpc());
        clusterStack.addDependency(vpc);

        RdsStack rdsStack = new RdsStack(app, "database-products", vpc.getVpc());
        rdsStack.addDependency(vpc);

        SnsStack snsStack = new SnsStack(app, "SNS");

        ServiceALBStack serviceALBStack = new ServiceALBStack(app, "load-balancing-ecommerce",
                clusterStack.getCluster(), snsStack.getProductEventTopic());
        serviceALBStack.addDependency(clusterStack);
        serviceALBStack.addDependency(rdsStack);
        serviceALBStack.addDependency(snsStack);

        app.synth();
    }
}