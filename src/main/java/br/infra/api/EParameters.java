package br.infra.api;

public enum EParameters {
    ECOMMERCE_SERVICE("ecommerce-service"),
    DOCKER_API_PRODUCTS("aclecioscruz/api-products");

    private String value;

    EParameters(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
