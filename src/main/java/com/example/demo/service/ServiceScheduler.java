package com.example.demo.service;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

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
        //-------------------------------------------------------------------------------------
        // deployment.getStatus().getAvailableReplicas() más conservador ya que es el numero real de replicas arrancadas
        // deploy.getSpec().getReplicas() más estatico el numero de pods puede variar debido a reninicios, pero a la larga es el valor esperado.

        try {
            // ------------------------------------
            // ApiGW section
            V1DeploymentList deployListApiGw = getDeployList(localProperties.getApigwNamespace(), localProperties.getApigwLables());

            if (deployListApiGw.getItems().size() > 1) {
                log.warn("There are two deployments in this namespace {} and with this lables {}", localProperties.getApigwNamespace(), localProperties.getApigwLables());
            } else if (deployListApiGw.getItems().isEmpty()) {
                log.warn("No deployment in this namespace {} and with this lables {}", localProperties.getApigwNamespace(), localProperties.getApigwLables());
            } else {
                log.info("#######################################################################################");
                V1Deployment deployment = deployListApiGw.getItems().get(0);
                log.info("ApiGw Replicas Available: " + deployment.getStatus().getAvailableReplicas());
                log.info("ApiGw Replicas: " + deployment.getSpec().getReplicas());
            }

            // ------------------------------------
            // HPA section (for each route)
            //
            V2beta1HorizontalPodAutoscalerList hpaList = getHPAList(localProperties.getNamespace(), localProperties.getLables());
            V1DeploymentList deployList = getDeployList(localProperties.getNamespace(), localProperties.getLables());
            log.info("HPA list size: " + hpaList.getItems().size());
            log.info("Deploy list size: " + deployList.getItems().size());


            for (Map<String,DataProperty> map : localProperties.getApigwProperties()) {
                Boolean foundHPABoolean = false;
                Boolean foundDeployBoolean = false;

                for (Entry<String, DataProperty> key : map.entrySet()) {
                    DataProperty objectString = key.getValue();
                    log.info("key:" + key.getKey());
                    log.info("object:" + objectString);

                    for (V2beta1HorizontalPodAutoscaler objectMeta : hpaList.getItems()) {
                        if (objectString.getName().equals(objectMeta.getSpec().getScaleTargetRef().getName())) {
                            log.info("--------------------------------------------------------------");
                            log.info("HPA MaxReplicas:" + objectMeta.getSpec().getMaxReplicas());
                            log.info("HPA MinReplicas:" + objectMeta.getSpec().getMinReplicas());
                            log.info("HPA CurrentReplicas:" + objectMeta.getStatus().getCurrentReplicas());
                            log.info("HPA nameDeploy:" + objectMeta.getSpec().getScaleTargetRef().getName());
                            foundHPABoolean = true;
                            if (objectMeta.getStatus().getCurrentReplicas() < objectMeta.getSpec().getMaxReplicas()) {
                                Integer thortllingUpdated = (objectMeta.getStatus().getCurrentReplicas() + 1) * objectString.getLimit();
                                Integer thortllingMax = (objectMeta.getSpec().getMaxReplicas()) * objectString.getLimit();
                                log.info("ThortllingKey: {} ThortllingDefault: {}  ThortllingNext: {} ThortllingMax: {}",
                                        key.getKey(),
                                        objectString.getLimit(),
                                        thortllingUpdated,
                                        thortllingMax);
                            } else {
                                Integer thortllingMax = (objectMeta.getSpec().getMaxReplicas()) * objectString.getLimit();
                                log.info("ThortllingKey: {} ThortllingDefault: {}  ThortllingNext: {} ThortllingMax: {}",
                                        key.getKey(),
                                        objectString.getLimit(),
                                        thortllingMax,
                                        thortllingMax);
                            }
                        }
                    }

                    if (Boolean.FALSE.equals(foundHPABoolean)) {
                        for (V1Deployment deploy : deployList.getItems()) {
                            if (objectString.getName().equals(deploy.getMetadata().getName())) {
                                foundDeployBoolean = true;
                                log.info("--------------------------------------------------------------");
                                log.info("Deployment Name:" + deploy.getMetadata().getName());
                                log.info("Deployment Replicas:" + deploy.getSpec().getReplicas());
                                log.info("Deployment Replicas Available:" + deploy.getStatus().getAvailableReplicas());
                                Integer thortllingMax = (deploy.getSpec().getReplicas()) * objectString.getLimit();
                                log.info("ThortllingKey: {} ThortllingDefault: {}  ThortllingMax: {}",
                                        key.getKey(),
                                        objectString.getLimit(),
                                        thortllingMax);
                            }
                        }
                    }

                    if (Boolean.FALSE.equals(foundHPABoolean) && Boolean.FALSE.equals(foundDeployBoolean) ) {
                        log.info("--------------------------------------------------------------");
                        log.error("RouteKey without HPA or Deployment: {}", key.toString());
                    }
                }
            }

        } catch (ApiException e) {
            // TODO: handle exception
            log.error(e.getMessage());
        } catch (IOException e) {
            // TODO: handle exception
            log.error(e.getMessage());
        } catch (Exception e) {
            // TODO: handle exception
            log.error(e.getMessage());
        } finally {

        }

    }

    public V1PodList getPodsList(String namespace, String labelSelector) throws ApiException, IOException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();
        return api.listNamespacedPod(namespace, null, null, null, null, labelSelector, null, null, null, null, null);
    }

    public V2beta1HorizontalPodAutoscalerList getHPAList(String namespace, String labelSelector)
            throws ApiException, IOException {
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
