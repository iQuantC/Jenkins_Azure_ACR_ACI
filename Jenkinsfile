pipeline {
    agent any
    tools {
	jdk 'java17015'
	maven 'maven387'
    }
    environment {
	SONAR_SCANNER_HOME = tool 'sonar7'
	IMAGE_NAME = "java-app"
        IMAGE_TAG = "${BUILD_NUMBER}"
	ACR_NAME = "javaapprepo00"
	ACR_LOGIN_SERVER = "${ACR_NAME}.azurecr.io"
	RESOURCE_GROUP = "iquant-00"
	ACI_NAME = "java-app-container"
	ACI_REGION = "eastus"
    }
    stages {
        stage('Initialize Pipeline'){
            steps {
                echo 'Initializing Pipeline ...'
		sh 'java -version'
		sh 'mvn -version'
            }
        }
        stage('Checkout GitHub Codes'){
            steps {
                echo 'Checking out GitHub Codes'
		checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[credentialsId: 'jenkins-git', url: 'https://github.com/iQuantC/Jenkins_Azure_ACR_ACI.git']])
            }
        }
        stage('Maven Build'){
            steps {
                echo 'Building Java App with Maven'
		sh 'mvn clean package'
            }
        }
        stage('JUnit Test of Java App'){
            steps {
                echo 'JUnit Testing'
		sh 'mvn test'
            }
        }
        stage('SonarQube Analysis'){
            steps {
                echo 'Running Static Code Analysis with SonarQube'
		withCredentials([string(credentialsId: 'sonartoken', variable: 'sonarToken')]) {
    			withSonarQubeEnv('sonar') {
    			   	sh '''
					${SONAR_SCANNER_HOME}/bin/sonar-scanner \
  					-Dsonar.projectKey=jenkinsazure \
  					-Dsonar.sources=. \
  					-Dsonar.host.url=http://172.18.0.3:9000 \
       					-Dsonar.java.binaries=target/classes \
  					-Dsonar.token=$sonarToken
   				'''
		   }
		}
            }
        }
        stage('Trivy FS Scan'){
            steps {
                echo 'Scanning File System with Trivy FS ...'
		sh 'trivy fs --format table -o FSScanReport.html'
            }
        }
        stage('Build & Tag Docker Image'){
            steps {
                echo 'Building the Java App Docker Image'
		script {
			sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
		}
            }
        }
        stage('Trivy Security Scan'){
            steps {
                echo 'Scanning Docker Image with Trivy'
		sh '''
  			trivy --severity HIGH,CRITICAL --cache-dir ${WORKSPACE}/.trivy-cache --no-progress --format table -o trivyFSScanReport.html image ${IMAGE_NAME}:${IMAGE_TAG}
     		'''
            }
        }
	stage('Authenticate with Azure') {
            steps {
		withCredentials([file(credentialsId: 'azServicePrincipal', variable: 'AZURE_CRED')]) {
			sh '''
				echo 'Authenticating with Azure'
    				az login --service-principal --username $(jq -r .clientId $AZURE_CRED) \
              					--password $(jq -r .clientSecret $AZURE_CRED) \
              					--tenant $(jq -r .tenantId $AZURE_CRED) > /dev/null
            			az account set --subscription $(jq -r .subscriptionId $AZURE_CRED)
			'''
		}
		
            }
        }
	stage('Tag & Push Image to Azure Container Registry (ACR)') {
            steps {
		withCredentials([file(credentialsId: 'azServicePrincipal', variable: 'AZURE_CRED')]) {
			sh '''
				echo 'Tagging and Pushing Image to ACR'
    				az acr login --name $ACR_NAME

          			echo "Tagging Docker image..."
          			docker tag $IMAGE_NAME:$IMAGE_TAG $ACR_LOGIN_SERVER/$IMAGE_NAME:$IMAGE_TAG

          			echo "Pushing Docker image to ACR..."
          			docker push $ACR_LOGIN_SERVER/$IMAGE_NAME:$IMAGE_TAG
       			'''
		}
            }
        }
	stage('Deploy to Azure Container Instance (ACI) & Get App URL') {
	    steps {
		withCredentials([file(credentialsId: 'azServicePrincipal', variable: 'AZURE_CRED')]) {
			sh '''
				echo 'Deploying Image to ACI'
				az container create \
            					--name $ACI_NAME \
            					--resource-group $RESOURCE_GROUP \
            					--image $ACR_LOGIN_SERVER/$IMAGE_NAME:$IMAGE_TAG \
            					--registry-login-server $ACR_LOGIN_SERVER \
            					--registry-username $(az acr credential show --name $ACR_NAME --query username -o tsv) \
            					--registry-password $(az acr credential show --name $ACR_NAME --query passwords[0].value -o tsv) \
            					--dns-name-label java-app-${BUILD_NUMBER} \
            					--ports 8090 \
            					--location $ACI_REGION \
		 				--os-type Linux \
       						--cpu 1 \
  						--memory 1.5 \
		 				--restart-policy Never

       					echo "Waiting for ACI to initialize..."
          				sleep 30

       					echo "Getting the URL of the ACI app..."
          				APP_URL=$(az container show \
            					--resource-group $RESOURCE_GROUP \
            					--name $ACI_NAME \
            					--query ipAddress.fqdn \
            					--output tsv):8090

          				echo "Application URL: http://$APP_URL"
    			'''
			}
		    }
	    }
	}
}
