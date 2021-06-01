package com.example.demo.service;

import java.io.IOException;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.config.DataProperty;
import com.example.demo.config.LocalProperties;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AutoscalingV2beta1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V2beta1HorizontalPodAutoscaler;
import io.kubernetes.client.openapi.models.V2beta1HorizontalPodAutoscalerList;
import io.kubernetes.client.util.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@EnableScheduling
@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceScheduler {
    
    private final LocalProperties localProperties;
    
    @Scheduled(fixedDelayString = "PT10S")
    public void scheduledExecution() {
        
        try {
            //------------------------------------
            // ApiGW section
            V1DeploymentList deployListApiGw = getDeployList(localProperties.getApigwNamespace(),localProperties.getApigwLables());
            
            if (deployListApiGw.getItems().size() > 1) {
                log.warn("There are two deployments in this namespace {} and with this lables {}",localProperties.getApigwNamespace(), localProperties.getApigwLables() );
            } else if (deployListApiGw.getItems().isEmpty()) {
                log.warn("No deployment in this namespace {} and with this lables {}",localProperties.getApigwNamespace(), localProperties.getApigwLables() );
            } else {
                log.info("--------------------------------------------------------------");
                V1Deployment deployment = deployListApiGw.getItems().get(0);
                log.info("ApiGw Replicas Available: " + deployment.getStatus().getAvailableReplicas()); 
                //log.info("STATUS: " + deployment.getStatus());
            }

            //------------------------------------
            // HPA section (for each route)
            //
            V2beta1HorizontalPodAutoscalerList hpaList = getHPAList(localProperties.getNamespace(),localProperties.getLables());
            for (V2beta1HorizontalPodAutoscaler objectMeta : hpaList.getItems()) {
                log.info("--------------------------------------------------------------");
                log.info("HPA MaxReplicas:" + objectMeta.getSpec().getMaxReplicas());
                log.info("HPA MaxReplicas:" + objectMeta.getSpec().getMinReplicas());
                log.info("HPA CurrentReplicas:" + objectMeta.getStatus().getCurrentReplicas());
                log.info("HPA nameDeploy:" + objectMeta.getSpec().getScaleTargetRef().getName());
                
                for ( DataProperty dataProperty : localProperties.getApigwProperties()) {
                    if (dataProperty.getRoute().equals(objectMeta.getSpec().getScaleTargetRef().getName())) {
                        if (objectMeta.getStatus().getCurrentReplicas() < objectMeta.getSpec().getMaxReplicas()) {
                            Integer thortllingUpdated = (objectMeta.getStatus().getCurrentReplicas()+1)*dataProperty.getThrottlingLimit();
                            Integer thortllingMax = (objectMeta.getSpec().getMaxReplicas())*dataProperty.getThrottlingLimit();
                            log.info("ThortllingKey: {} ThortllingDefault: {}  ThortllingNext: {} ThortllingMax: {}",dataProperty.getThrottlingKey(),dataProperty.getThrottlingLimit(),thortllingUpdated, thortllingMax);
                        } else {
                            Integer thortllingMax = (objectMeta.getSpec().getMaxReplicas())*dataProperty.getThrottlingLimit();
                            log.info("ThortllingKey: {} ThortllingDefault: {}  ThortllingNext: {} ThortllingMax: {}",dataProperty.getThrottlingKey(),dataProperty.getThrottlingLimit(),thortllingMax,thortllingMax);
                        }
                    }
                }
                
            }
            
//            V1DeploymentList deployList = getDeployList(localProperties.getNamespace(),localProperties.getLables());
//            for (V1Deployment objectMeta : deployList.getItems()) {
//                log.info("--------------------------------------------------------------");
//                //log.info( "Deployment replicas:" + objectMeta.getSpec());
//                log.info( "Deployment replicas:" + objectMeta.getStatus());
//            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        
    }
    
    public V1PodList getPodsList(String namespace, String labelSelector) throws ApiException, IOException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();
        return api.listNamespacedPod(namespace, null, null, null, null, labelSelector, null, null, null, null, null);
    }
    
    public V2beta1HorizontalPodAutoscalerList getHPAList(String namespace, String labelSelector) throws ApiException, IOException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        AutoscalingV2beta1Api api = new AutoscalingV2beta1Api();
        return api.listNamespacedHorizontalPodAutoscaler(namespace, null, null, null, null, labelSelector, null, null, null, null, null);
    }
    
    public V1DeploymentList getDeployList(String namespace, String labelSelector) throws ApiException, IOException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        AppsV1Api api = new AppsV1Api();
        return api.listNamespacedDeployment(namespace, null, null, null, null, labelSelector, null, null, null, null, null);
    }

}
