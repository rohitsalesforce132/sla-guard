package com.slaguard.tmf;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TMF622: Product Ordering API
 * Handles ordering of network slice services
 */
@Path("/tmf-api/productOrdering/v4")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class TMF622Resource {

    @ConfigProperty(name = "sla-guard.tmf.base-url")
    String tmfBaseUrl;

    /**
     * Create a new product order for a network slice
     */
    @POST
    @Path("/productOrder")
    public Response createProductOrder(ProductOrderRequest request) {
        Log.infof("TMF622: Creating product order - channel=%s", request.channel);

        try {
            ProductOrderResponse order = new ProductOrderResponse();
            order.id = "PO-" + UUID.randomUUID().toString();
            order.orderDate = LocalDateTime.now();
            order.channel = request.channel;
            order.state = "ACKNOWLEDGED";
            order.href = tmfBaseUrl + "/productOrdering/v4/productOrder/" + order.id;

            // Process order items
            for (ProductOrderItem item : request.orderItem) {
                OrderItemResponse itemResponse = new OrderItemResponse();
                itemResponse.id = "POI-" + UUID.randomUUID().toString();
                itemResponse.productId = item.product.id;
                itemResponse.productName = item.product.name;
                itemResponse.quantity = item.quantity;
                itemResponse.state = "ACKNOWLEDGED";
                order.orderItem.add(itemResponse);
            }

            Log.infof("Product order created: %s with %d items", order.id, order.orderItem.size());
            return Response.status(201).entity(order).build();

        } catch (Exception e) {
            Log.errorf("Error creating product order: %s", e.getMessage(), e);
            return Response.serverError()
                    .entity(createErrorResponse("ORDER_CREATION_ERROR", e.getMessage()))
                    .build();
        }
    }

    /**
     * Get a product order by ID
     */
    @GET
    @Path("/productOrder/{id}")
    public Response getProductOrder(@PathParam("id") String id) {
        Log.debugf("TMF622: Getting product order %s", id);

        ProductOrderResponse order = new ProductOrderResponse();
        order.id = id;
        order.orderDate = LocalDateTime.now().minusHours(1);
        order.channel = "web";
        order.state = "COMPLETED";
        order.href = tmfBaseUrl + "/productOrdering/v4/productOrder/" + id;

        return Response.ok(order).build();
    }

    /**
     * List product orders
     */
    @GET
    @Path("/productOrder")
    public Response listProductOrders(
            @QueryParam("state") String state,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        Log.debugf("TMF622: Listing product orders - state=%s", state);

        ProductOrderListResponse response = new ProductOrderListResponse();

        // Return mock orders for demo
        for (int i = 0; i < 3; i++) {
            ProductOrderResponse order = new ProductOrderResponse();
            order.id = "PO-" + (1000 + i);
            order.orderDate = LocalDateTime.now().minusHours(i + 1);
            order.channel = "web";
            order.state = state != null ? state : "COMPLETED";
            order.href = tmfBaseUrl + "/productOrdering/v4/productOrder/" + order.id;
            response.add(order);
        }

        return Response.ok(response).build();
    }

    /**
     * Cancel a product order
     */
    @POST
    @Path("/productOrder/{id}/cancel")
    public Response cancelProductOrder(@PathParam("id") String id) {
        Log.infof("TMF622: Cancelling product order %s", id);

        ProductOrderResponse order = new ProductOrderResponse();
        order.id = id;
        order.state = "CANCELLED";
        order.href = tmfBaseUrl + "/productOrdering/v4/productOrder/" + id;

        return Response.ok(order).build();
    }

    private TMFErrorResponse createErrorResponse(String code, String message) {
        TMFErrorResponse error = new TMFErrorResponse();
        error.code = code;
        error.message = message;
        error.reason = message;
        error.status = 500;
        error.timestamp = LocalDateTime.now();
        return error;
    }

    // TMF622 Data Models

    public static class ProductOrderRequest {
        public String channel;
        public String priority;
        public String requestedCompletionDate;
        public List<ProductOrderItem> orderItem = new ArrayList<>();
        public List<TMFCharacteristic> characteristic = new ArrayList<>();
    }

    public static class ProductOrderItem {
        public int id;
        public ProductRef product;
        public int quantity = 1;
        public Map<String, Object> productOffering = new HashMap<>();
    }

    public static class ProductRef {
        public String id;
        public String name;
        public String href;
    }

    public static class ProductOrderResponse {
        public String id;
        @com.fasterxml.jackson.annotation.JsonProperty("@type")
        public String type = "ProductOrder";
        public LocalDateTime orderDate;
        public String channel;
        public String state; // ACKNOWLEDGED, HOLDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
        public String href;
        public List<OrderItemResponse> orderItem = new ArrayList<>();
    }

    public static class OrderItemResponse {
        public String id;
        public String productId;
        public String productName;
        public int quantity;
        public String state;
    }

    public static class ProductOrderListResponse extends ArrayList<ProductOrderResponse> {
    }
}
