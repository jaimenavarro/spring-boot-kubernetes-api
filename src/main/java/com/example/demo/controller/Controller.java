package com.example.demo.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.LocalProperties;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import lombok.Data;

@RestController
@Data
public class Controller {
    
    private static final String PATH = "kubernetes/";
    private final LocalProperties localProperties;
    
    
    @GetMapping(path = PATH + "test" )
    public String test() {
        return localProperties.toString();
    }
    
    @GetMapping(path = PATH + "pods/{namespace}" )
    public List<V1ObjectMeta> deploymentGet( @PathVariable String namespace ) throws ApiException {

        ApiClient client = Config.fromToken("https://kubernetes.default", "eyJhbGciOiJSUzI1NiIsImtpZCI6IkZFb0UtQWZQQW9pUGgxRlphLW5fVnVvVnFiUWNJVWlQaWFtOUxPMVlITW8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJzZHAiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlY3JldC5uYW1lIjoic2RwLXRva2VuLWc4Z3JqIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6InNkcCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjhmZmQxMDJkLTViZDctNGJiOS1iZGFjLThjOWZhMzM4ZDYyMyIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpzZHA6c2RwIn0.xByTBsXXHV9HLcEVHB59F0RmhueXm02aOU-pbHBYndqPEXU85Kk81HasC1bWUy1ZNEi7fKd7dv6EIE-2kPgge-nbGsqZ_tx0R7fj7XZ9JajOjyttcP5530tPdKESyaY94X6C2twMW-3s0XgcvrEmK8b6qix-Tny2_DXs8tD9OuS5PGSHcdJlQDU5wR_vuY-e9HIIxmIsXG_ccxH5g_0uQkyZhzIaQdMj97c3dVuVpFVfTEjp2Ij7Q8IgZrCs9-c-_vUbZqGXpw-gc1iqKie6dL_KH6yWFNaEElnIgXrX1TFUPDRXX39SERc1B6R-9BnAXvLB7_mc2CXn41yy5fFkFw", false);
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();
        V1PodList list = api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null);
        ArrayList<V1ObjectMeta> listPods = new ArrayList<>();
        for (V1Pod item : list.getItems()) {
            listPods.add(item.getMetadata());
        }
        
        
        return listPods;
    }
    
    @GetMapping(path = PATH + "pods2/{namespace}" )
    public List<V1ObjectMeta> deploymentGet2( @PathVariable String namespace ) throws ApiException, IOException {

        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();
        V1PodList list = api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null);
        ArrayList<V1ObjectMeta> listPods = new ArrayList<>();
        for (V1Pod item : list.getItems()) {
            listPods.add(item.getMetadata());
        }
        
        return listPods;
    }
    
    
    @GetMapping(path = PATH + "pods3/{namespace}" )
    public List<V1ObjectMeta> deploymentGet3( @PathVariable String namespace ) throws ApiException, IOException {

        ApiClient client = ClientBuilder.cluster().build();
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();
        V1PodList list = api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null);
        ArrayList<V1ObjectMeta> listPods = new ArrayList<>();
        for (V1Pod item : list.getItems()) {
            listPods.add(item.getMetadata());
        }
        
        return listPods;
    }
    
    
    @GetMapping(path = PATH + "pods4/{namespace}" )
    public List<V1ObjectMeta> deploymentGet4( @PathVariable String namespace ) throws ApiException, IOException {

        ApiClient client = ClientBuilder.oldCluster().build();
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();
        V1PodList list = api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null);
        ArrayList<V1ObjectMeta> listPods = new ArrayList<>();
        for (V1Pod item : list.getItems()) {
            listPods.add(item.getMetadata());
        }
        
        return listPods;
    }


}
