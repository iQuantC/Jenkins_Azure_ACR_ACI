pipeline {
    agent any
    tools {
	jdk 'java17015'
	maven 'maven387'
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
            }
        }
        stage('Trivy FS Scan'){
            steps {
                echo 'Scanning File System with Trivy FS ...'
            }
        }
        stage('Build & Tag Docker Image'){
            steps {
                echo 'Building the Java App Docker Image'
            }
        }
        stage('Trivy Security Scan'){
            steps {
                echo 'Scanning Docker Image with Trivy'
            }
        }
		stage('Authenticate with Azure') {
            steps {
				echo 'Authenticating with Azure'
            }
        }
		stage('Tag & Push Image to Azure Container Registry') {
            steps {
				echo 'Tagging and Pushing Image to ACR'
            }
        }
	    stage('Deploy to Azure Container Instance & Get App URL') {
		    steps {
				echo 'Deploying Image to ACI'
		    }
	    }
	}
}
