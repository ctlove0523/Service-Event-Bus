# Service-Event-Bus
An event bus can be used between instances of one service

## Service instance discovery strategy

### Base on Headless Services and DNS

#### Define a headless service

spring-cloud-gateway-headless.yaml

~~~yaml
apiVersion: v1
kind: Service
metadata:
  name: spring-cloud-gateway-headless
  labels:
    app: gateway
spec:
  ports:
    - name: gateway-headless-service
      port: 8080
      targetPort: 5225
  # This must set be None    
  clusterIP: None
  selector:
    trust-service: gateway
~~~



#### Use kubectl create headless service

~~~
kubectl create -f spring-cloud-gateway-headless.yaml
~~~

When service has configured selectors like in spring-cloud-gateway-headless.yaml,DNS is automatically configured and we can use DNS mechanism to find address of pod's IP.

### Base kubernetes service and use API to find instances.

* API Access Control

  * Create a service account in default name space

    ~~~
    kubectl create serviceaccount chentong
    ~~~

  * Bind cluster-admin cluster role to chentong account

    ~~~
    kubectl create clusterrolebinding chentong-rolebinding --clusterrole=cluster-admin --serviceaccount=default:chentong
    ~~~

  * get associated secret:

    ~~~
    kubectl get serviceaccounts chentong -o yaml
    ~~~

    Output:

    ~~~
    apiVersion: v1
    kind: ServiceAccount
    metadata:
      ...
    secrets:
    - name: chentong-token-v8lrh
    ~~~

    

  * Get secret 

    ~~~
    kubectl get secret chentong-token-v8lrh -o yaml
    ~~~

    Output:

    ~~~
    apiVersion: v1
    data:
      ca.crt: {ca}
      namespace: ZGVmYXVsdA==
      token: {token}
    kind: Secret
    metadata:
      annotations:
        kubernetes.io/service-account.name: chentong
        kubernetes.io/service-account.uid: e88a5db9-1780-11eb-8bc8-fa163edda8e8
      creationTimestamp: 2020-10-26T11:46:38Z
      name: chentong-token-v8lrh
      namespace: default
      resourceVersion: "66194811"
      selfLink: /api/v1/namespaces/default/secrets/chentong-token-v8lrh
      uid: e88c199d-1780-11eb-b83d-fa163e30f7ea
    type: kubernetes.io/service-account-token
    ~~~

  * In our code we use token  to call kubernetes API, use the follow command to get JWT Token

    ~~~
    kubectl get secret chentong-token-v8lrh -n default -o jsonpath={".data.token"} | base64 -d
    ~~~

    
