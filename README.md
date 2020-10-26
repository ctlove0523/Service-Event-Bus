# Service-Event-Bus
An event bus can be used between instances of one service

# Service-Event-Bus
An event bus can be used between instances of one service



## Service instance discovery strategy

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

    
