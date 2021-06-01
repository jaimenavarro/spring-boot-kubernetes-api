package com.example.demo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AutoscalingApi;
import io.kubernetes.client.openapi.apis.AutoscalingV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1HorizontalPodAutoscaler;
import io.kubernetes.client.openapi.models.V1HorizontalPodAutoscalerList;
import io.kubernetes.client.openapi.models.V1HorizontalPodAutoscalerStatus;
import io.kubernetes.client.openapi.models.V1ListMeta;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import lombok.Data;

@RestController
@Data
public class ControllerOutside {
    
    private static final String PATH = "kubernetes/ext/";
    private final LocalProperties localProperties;
    
    
    @GetMapping(path = PATH + "pod")
    public List<V1ObjectMeta> pods() throws ApiException, IOException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();
        V1PodList list = api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
        ArrayList<V1ObjectMeta> listPods = new ArrayList<>();
        for (V1Pod item : list.getItems()) {
            listPods.add(item.getMetadata());
        }
        return listPods;
    }
    
    @GetMapping(path = PATH + "hpa_names")
    public List<String> hpaNames() throws ApiException, IOException{
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        AutoscalingV1Api api = new AutoscalingV1Api();
        V1HorizontalPodAutoscalerList list = api.listHorizontalPodAutoscalerForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
        
        ArrayList<String> listHPA = new ArrayList<>();
        for (V1HorizontalPodAutoscaler item : list.getItems()) {
            listHPA.add(item.getMetadata().getName());
        }
        return listHPA;
    }
    
    @GetMapping(path = PATH + "hpa")
    public List<V1HorizontalPodAutoscaler> hpa() throws ApiException, IOException{
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        AutoscalingV1Api api = new AutoscalingV1Api();
        V1HorizontalPodAutoscalerList list = api.listHorizontalPodAutoscalerForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
        
        ArrayList<V1HorizontalPodAutoscaler> listHPA = new ArrayList<>();
        for (V1HorizontalPodAutoscaler item : list.getItems()) {
            listHPA.add(item);
        }
        return listHPA;
    }
    
    
    @GetMapping(path = PATH + "hpa/{namespace}/{name}" )
    public V1HorizontalPodAutoscalerStatus hpaGet( @PathVariable String name, @PathVariable String namespace ) throws ApiException, IOException{
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        AutoscalingV1Api api = new AutoscalingV1Api();
        return api.readNamespacedHorizontalPodAutoscaler(name, namespace, null, null, null).getStatus();
    }
    
    
    @GetMapping(path = PATH + "pods/{namespace}" )
    public List<V1ObjectMeta> deploymentGet( @PathVariable String namespace ) throws ApiException, IOException{
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

}
