package br.infra.api.databases;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.Collections;

public class RdsStack extends Stack {
    public RdsStack(final Construct scope, final String id, Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public RdsStack(final Construct scope, final String id, final StackProps props, Vpc vpc) {
        super(scope, id, props);

        CfnParameter databasePassword = CfnParameter.Builder.create(this, "databasePassword")
                .type("String")
                .description("database products api")
                .build();

        ISecurityGroup iSecurityGroup = SecurityGroup
                .fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(5432));

        DatabaseInstance databaseInstance = DatabaseInstance.Builder.create(this, "databaseProducts")
                .instanceIdentifier("projectApiProducts")
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_14)
                        .build()))
                .vpc(vpc)
                .credentials(Credentials.fromUsername("postgres",
                        CredentialsFromUsernameOptions.builder()
                                .password(SecretValue.unsafePlainText(databasePassword.getValueAsString()))
                                .build()))
                .instanceType(InstanceType.of(InstanceClass.T3, InstanceSize.MICRO))
                .multiAz(false)
                .allocatedStorage(10)
                .securityGroups(Collections.singletonList(iSecurityGroup))
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(vpc.getPrivateSubnets())
                        .build())
                .build();

        CfnOutput.Builder.create(this, "databaseEndPoint")
                .exportName("databaseEndPoint")
                .value(databaseInstance.getDbInstanceEndpointAddress())
                .build();

        CfnOutput.Builder.create(this, "dbPassword")
                .exportName("dbPassword")
                .value(databasePassword.getValueAsString())
                .build();


    }
}
