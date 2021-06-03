package com.example.demo.service;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

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
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V2beta1HorizontalPodAutoscaler;
import io.kubernetes.client.openapi.models.V2beta1HorizontalPodAutoscalerList;
import io.kubernetes.client.util.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@EnableScheduling
@Slf4j
@RequiredArgsConstructor
@Service
public class ServiceScheduler {

    private final LocalProperties localProperties;

    private V2beta1HorizontalPodAutoscalerList hpaList;
    private V1DeploymentList deployListApiGw;
    private V1DeploymentList deployList;
    private ApiClient client;

    @PostConstruct
    private void postConstruct() {
        try {
            client = Config.defaultClient();
            client.setConnectTimeout(10000);
            client.setReadTimeout(10000);
            Configuration.setDefaultApiClient(client);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Scheduled(initialDelayString = "${scheduler.initialDelayString}", fixedDelayString = "${scheduler.fixedDelayString}")
    public void scheduledExecution() {
        //-------------------------------------------------------------------------------------
        // TODO: Review
        // deployment.getStatus().getAvailableReplicas() más conservador ya que es el numero real de replicas arrancadas
        // deploy.getSpec().getReplicas() más estatico el numero de pods puede variar debido a reninicios, pero a la larga es el valor esperado.
        try {
            log.info("#######################################################################################");
            deployListApiGw = getDeployList(localProperties.getApigwNamespace(), localProperties.getApigwLables());
            hpaList = getHPAList(localProperties.getNamespace(), localProperties.getLables());
            deployList = getDeployList(localProperties.getNamespace(), localProperties.getLables());

            for (Map<String,DataProperty> map : localProperties.getApigwProperties()) {
                Boolean foundHPABoolean = false;
                Boolean foundDeployBoolean = false;
                for (Entry<String, DataProperty> key : map.entrySet()) {
                    foundHPABoolean = this.setLimitBasedOnHPA(key.getValue());
                    if (Boolean.FALSE.equals(foundHPABoolean)) {
                        foundDeployBoolean = this.setLimitBasedOnDeployment(key.getValue());
                    }
                    if (Boolean.FALSE.equals(foundHPABoolean) && Boolean.FALSE.equals(foundDeployBoolean) ) {
                        log.error("RouteKey without HPA or Deployment: {}", key.toString());
                    }

                    Integer numberApiGwinK8sInteger = setLimitsForEachApiGw();
                    Integer totalLimitPerApiGw = key.getValue().getTotalLimit() / numberApiGwinK8sInteger;
                    key.getValue().setTotalLimitPerApiGw(totalLimitPerApiGw);
                    log.info("key: {} dataProperty: {}", key.getKey(), key.getValue().toString());
                }
            }

        } catch (Exception e) {
            // TODO: handle exception
            log.error(e.getMessage());
            log.warn("Limits couldn't be updated: {}", localProperties.getApigwProperties());
        } finally {
        }
    }

    private Integer setLimitsForEachApiGw() {
        Integer apiGwReplicas = 1;
        if (deployListApiGw.getItems().size() > 1) {
            log.error("There are two deployments in this namespace {} and with this lables {}", localProperties.getApigwNamespace(), localProperties.getApigwLables());
        } else if (deployListApiGw.getItems().isEmpty()) {
            log.error("No deployment in this namespace {} and with this lables {}", localProperties.getApigwNamespace(), localProperties.getApigwLables());
        } else {
            V1Deployment deployment = deployListApiGw.getItems().get(0);
            log.info("ApiGw Replicas: {}, Replicas Available: {}",
                    deployment.getSpec().getReplicas(),
                    deployment.getStatus().getAvailableReplicas());
            apiGwReplicas = deployment.getSpec().getReplicas();
        }
        return apiGwReplicas;
    }

    private Boolean setLimitBasedOnHPA (DataProperty dataProperty) throws ApiException, IOException {
        for (V2beta1HorizontalPodAutoscaler hpa : hpaList.getItems()) {
            if (dataProperty.getName().equals(hpa.getSpec().getScaleTargetRef().getName())) {
                log.info("HPA MaxReplicas: {}, MinReplicas: {}, CurrentReplicas: {}, ScaleTargetRef: {}",
                        hpa.getSpec().getMaxReplicas(),
                        hpa.getSpec().getMinReplicas(),
                        hpa.getStatus().getCurrentReplicas(),
                        hpa.getSpec().getScaleTargetRef().getName()
                        );

                if ((hpa.getStatus().getCurrentReplicas() < hpa.getSpec().getMaxReplicas())
                        && Boolean.TRUE.equals(dataProperty.getDinamicLimit())) {
                    Integer totalLimit = (hpa.getStatus().getCurrentReplicas() + 1) * dataProperty.getLimitPerPod();
                    dataProperty.setTotalLimit(totalLimit);
                } else {
                    Integer totalLimit = (hpa.getSpec().getMaxReplicas()) * dataProperty.getLimitPerPod();
                    dataProperty.setTotalLimit(totalLimit);
                }
                return true;
            }
        }
        return false;
    }


    private Boolean setLimitBasedOnDeployment(DataProperty dataProperty) throws ApiException, IOException {
        for (V1Deployment deploy : deployList.getItems()) {
            if (dataProperty.getName().equals(deploy.getMetadata().getName())) {
                log.info("Deployment Name: {}, Replicas: {}, Replicas Available: {}",
                        deploy.getMetadata().getName(),
                        deploy.getSpec().getReplicas(),
                        deploy.getStatus().getAvailableReplicas());

                Integer thortllingMax = (deploy.getSpec().getReplicas()) * dataProperty.getLimitPerPod();
                dataProperty.setTotalLimit(thortllingMax);
                return true;
            }
        }
        return false;
    }

    private V2beta1HorizontalPodAutoscalerList getHPAList(String namespace, String labelSelector)
            throws ApiException {
        AutoscalingV2beta1Api api = new AutoscalingV2beta1Api();
        return api.listNamespacedHorizontalPodAutoscaler(namespace, null, null, null, null, labelSelector, null, null, null, null, null);
    }

    private V1DeploymentList getDeployList(String namespace, String labelSelector) throws ApiException {
        AppsV1Api api = new AppsV1Api();
        return api.listNamespacedDeployment(namespace, null, null, null, null, labelSelector, null, null, null, null, null);
    }

}
