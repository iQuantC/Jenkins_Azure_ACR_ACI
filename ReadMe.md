# Deploy App to Azure Container Instance using Jenkins CI/CD Pipeline


## Requirements
1. Java:        Language & Framework 
2. Spring Boot: Creates Web Applications in Java
2. Maven:       Java Application Build Tool
3. Shell:       Commands to Build, Test, and Deploy
3. Jenkins:     CI/CD Pipeline Tool
4. Docker:      Containerization Tool
5. Microsoft Azure Account & Azure CLI


### Test the Application Locally
Install Java
```sh
sudo apt update
sudo apt install openjdk-11-jre-headless
java -version
```

Install Maven
```sh
sudo apt update
sudo apt install maven
mvn -version
```

### Build the Application
```sh
mvn clean package
```
This creates 2 JAR files in the target directory:
1. hello-world-1.0-SNAPSHOT.jar - Executable JAR file with all dependencies included
2. hello-world-1.0-SNAPSHOT.jar.original - Original Non-executable JAR without dependencies (back up of JAR file before Spring Boot processed it)


### Run the Tests
```sh
mvn test
```


### Additional Configuration (Optional)
If you want to change the port from the default 8080, create a file - src/main/resources/application.properties
with the following content:
```sh
server.port=8090
```

### Run The App Locally
To run the Java App Locally, run the command below in a dedicated terminal:
```sh
java -jar target/hello-world-1.0-SNAPSHOT.jar --server.port=8090
```

Open your browser to: 
```sh
http://localhost:8090
```
Ctrl + C to exit


## Dockerize the Java App
### Build the Docker image
```sh
docker images
```

I'm gonna use the port 8090
```sh
docker build -t java-app .
```

### Run the Docker container
```sh
docker run -p 8090:8090 java-app
```

Open your browser to: 
```sh
http://localhost:8090
```
Ctrl + C to exit


## Set up Jenkins with a Container
We want to build a docker image later with our Jenkins Pipeline, so we need a Jenkins Container or VM with Docker in it (i.e., Docker-in-Docker or DinD)

### Create a Docker Network
```sh
docker network ls
```
```sh
docker network create jenkins-java
```

```sh
docker run -d --name jenkins-dind \
-p 8080:8080 \
-p 50000:50000 \
-v /var/run/docker.sock:/var/run/docker.sock \
-v $(which docker):/usr/bin/docker \
-u root \
-e DOCKER_GID=$(getent group docker | cut -d: -f3) \
--network jenkins-java \
jenkins/jenkins:lts
```

### Check the Docker Processes Running
```sh
docker ps
```
Get Jenkins Container IP Address
```sh
docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' jenkins-dind
```
On your browser, open
```sh
ContainerIP:8080
```

Note: If you see the error "It appears that your reverse proxy set up is broken.", go to Manage Jenkins > System and change the Jenkins URL from localhost:8080 to ContainerIP:8080.


### Log into the Jenkins Container
```sh
docker exec -it jenkins-dind bash
```

### Set up Docker in the Jenkins container
Inside the container, run the following commands: 
```sh
docker --version
```

```sh
groupadd -for -g $DOCKER_GID docker
usermod -aG docker jenkins
exit
```

```sh
docker ps
```

```sh
docker restart jenkins-dind
```

### Check the Logs of the Jenkins Container
This will also provide you the Jenkins Initial Password. Use the password to login to Jenkins & Install suggested plugins
```sh
docker logs -f jenkins-dind
```
Ctrl + C to exit logs

### Integrate Jenkins & GitHub Repository
In your GitHub Account, 
1. Create a PAT  with scope (repo & admin:repo_hook), and 
2. Add it as a Credential (Kind: Username with Psswd) on Jenkins UI
    1. Username:    GitHub Username here
    2. Password:    PAT here
3. Create Pipeline Job with Definition: "Pipeline script from SCM", and complete the fields SCM, Repository URL, Credentials, Branch Specifier, and Script Path. 
4. Apply and Save when done.


### Install Java & Maven in the Jenkins Container
```sh
docker exec -it jenkins-dind bash
```

Check Java version (Usually installed with Jenkins since Java is one of Jenkins' Dependencies) & JAVA_HOME
```sh
java -version
```
```sh
mvn -version
```

Install Maven & get MAVEN_HOME as well (usually: /usr/share/maven)
```sh
apt update -y
apt install maven -y
mvn -version
exit
```

***In the Jenkins GUI***
1. Install Maven Integration Plugin 
2. Set up Maven installation in Manage Jenkins > Tools > Maven installations - Uncheck "Install Automatically"
    1. Name:        maven387
    2. MAVEN_HOME:  /usr/share/maven
3. Set up JDK installation in Manage Jenkins > Tools > JDK installations - Uncheck "Install Automatically"
    1. Name:        java17015
    2. JAVA_HOME:   /opt/java/openjdk
4. Add these tools in the Jenkinsfile for 'jdk' and 'maven'.



## Set up SonarQube Analysis with a Container

Run the command below to create the SonarQube Container on the same Docker network as Jenkins:
```sh
docker run -d --name sonarqube-dind \
-e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
-p 9000:9000 \
--network jenkins-java \
sonarqube:latest
```

Check the logs of the SonarQube Container (Wait till SonarQube is Operational):
```sh
docker logs -f sonarqube-dind
```

### Check the Docker Processes Running
```sh
docker ps
```
Get SonarQube Container IP Address
```sh
docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' sonarqube-dind
```
On your browser, open
```sh
ContainerIP:9000
```

Login to SonarQube GUI with 
```sh
Username: admin
Password: admin
```
and Change the password.


### Login to Jenkins Container & Establish Communication to the SonarQube Container
```sh
docker exec -it jenkins-dind bash
```
Update & Install Ping in Jenkins Container once you've logged in:
```sh
apt-get update -y
apt-get install iputils-ping -y
exit
```


***Use Jenkins Container bash to Ping SonarQube Container***
```sh
docker exec -it jenkins-dind ping sonarqube-dind
```
You will see bytes of data coming in showing established connection between the 2 containers.



### Create a SonarQube Token and Add it to Jenkins Credentials:
1. Click on the User Account, and click on "My Account"
2. Go to Security, create a token of type "Global Analysis Token", expiry date, Name: jmsonar, and Generate.
3. Copy and Save the token somewhere safe.
4. Go to Jenkins Credentials, select Kind "Secret text", Paste sonar token as the secret and provide ID and description.
5. Install the "SonarQube Scanner" and "Sonar Quality Gates" Plugins.
6. Go to Jenkins > Manage Jenkins > Systems > SonarQube Installation, Name: sonar, SonarQube URL (http://ContainerIP:9000), and Server auth token: select the credential. Apply and Save.
7. Go to Jenkins > Manage Jenkins > Tools > Add Name: sonar7, Install Automatically. Save and Apply


### Create Project in SonarQube
Create a Local Project in SonarQube and provide the ff:
1. Project display name, 
2. Project key, 
3. GitHub branch (main), 
4. Use the global setting, and 
5. Create project. 
6. Click on "Locally", Use existing token and enter name of your sonar token as value "jmsonar" and continue
7. Run analysis on your project: Select Other & Linux. Copy the execution script and add to your pipeline script. We will modify it a bit later.**
8. SonarQube Installation Name or SonarQubeEnv: sonar (the setup we did @ Manage Jenkins > System), add to pipeline
9. SonarQube Tool Name: sonar7 (the setup we did @ Manage Jenkins > Tools), add to pipeline as an environment variable.
10. Create a sonar-project.properties file in root of your GitHub Repository and add the SonarQube project key and organization (GitHub Username). 
11. Since the application is Java-based, you must specify the location of the Java binaries which is usually stored in a "target/classes" directory. Add it to the SonarScanner block as: "-Dsonar.java.binaries=target/classes". 


***To Generate Jenkins Pipeline Script***
1. In Jenkins UI, go to configure the Jenkins Pipeline
2. Click on Pipeline Syntax
3. Sample Step: Select withCredentials, Add Bindings, Select Secret text, Variable: sonarToken, Credentials: select the credentials 
4. Generate Pipeline Script.


## Trivy File System Scan
Login to the Jenkins Container
```sh
docker exec -it jenkins-dind bash
```
Check if Trivy is installed
```sh
trivy --version
```

If not, install Trivy in the Jenkins Container
```sh
curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin v0.17.2
```

```sh
trivy --version
exit
```
Add Trivy FS Scan stage to the Jenkinsfile


## Build Docker Image
Make sure Docker is installed in the Jenkins Container and there is a Dockerfile in the root of the GitHub Repo
```sh
docker --version
```

In Jenkins UI,
1. Install Docker & Docker Pipeline plugins in Jenkins UI
2. Manage Jenkins > Tools > Add Docker Installations: Name: Docker, Install Automatically from docker.com
3. Apply and Save.



## Trivy Docker Image Scan
Trivy is already set up. 
Now, scan the docker image with Trivy.



## Deployment to Microsoft Azure

### Push docker image to ACR & Deploy to ACI
Make sure you have Azure subscription set up (a default one is created when you sign up). 


### Install Azure CLI on Jenkins Container
Login to the Jenkins Container
```sh
docker exec -it jenkins-dind bash
```

```sh
apt-get update -y
apt-get install -y ca-certificates curl apt-transport-https lsb-release gnupg jq
curl -sL https://aka.ms/InstallAzureCLIDeb | bash
```

Check that installation was successful:
```sh
az version 
exit
```

### Login to Azure via CLI
```sh
az login
```
1. This command will provide a link and code to authenticate on a browser.
2. Select a number for the subscription you want to use (e.g., 1)
3. Close the authentication page on your browser.
4. With the Subscription ID, create an Azure Service Principal


### Create Azure Service Principal
Contributor Role in Azure allows users to create, manage, and delete resources but can't delegate access like the "Owner" Role (Admin Role):
```sh
az ad sp create-for-rbac --name jenkins-sp --role Contributor \
  --scopes /subscriptions/<SUBSCRIPTION_ID> \
  --sdk-auth
```
You can verify it on Azure Portal in your Azure Subscription > Access ctrl (IAM) > Role assignments.


5. Copy the JSON output on the terminal after creating the Azure Service Principal in a .json file.
6. Create a "Secret file" credential in Jenkins Credentials for it.
7. Register your Azure subscription to use ContainerRegistry. First, check if it's registered:
```sh
az provider show --namespace Microsoft.ContainerRegistry --query "registrationState"
```
If not, then register it (& rerun previous command to check): 
```sh
az provider register --namespace Microsoft.ContainerRegistry
```

8. Create your ACR repo:

### Create Azure Container Registry (ACR)
Create a Resource Group (if you don't have one already). The ACR_NAME is the repo name (must be Globally unique, alphanumeric & lowercase):
```sh
az acr create --resource-group <RESOURCE_GROUP> \
  --name <ACR_NAME> --sku Basic
```
For example: 
```sh
az acr create --resource-group iquant-00   --name javaapprepo00 --sku Basic
```
On Azure Portal > Go to Container Registries and Refresh to verify


### Push Docker Image to ACR
In Jenkins > Pipeline Syntax > withCredentials > Bindings: Secret file, Variable: AZURE_CRED, Credentials: select the Azure credential from earlier > Generate Pipeline Script. Copy script to Jenkinsfile & edit. 

Once pushed, verify on ACR > open the javaapprepo00 > Go to Services & select Repositories > Click on the repo to see the pushed docker image.


### Deploy Java App Docker Image to ACI
Run the Jenkins pipeline to deploy


### Enable Admin Access to ACR
In case you get error with ACI pulling image from ACR, run this command:
```sh
docker exec -it jenkins-dind bash
```
```sh
az acr update -n javaapprepo00 --admin-enabled true
```

### Register your Azure subscription to use ContainerInstance. 
First, check if it's registered:
```sh
az provider show --namespace Microsoft.ContainerInstance --query "registrationState"
```
If not, then register it (& rerun previous command to check): 
```sh
az provider register --namespace Microsoft.ContainerInstance
```
On Azure Portal > Go to Container Instances and Refresh to verify > Settings and click Containers


### Setup Jenkins GitHub Webhooks
Automate Jenkins pipeline build on "git push"

1. On Jenkins, Go to the Jenkins job > Configure > Select the "GitHub hook trigger for GITScm polling" trigger > Apply & Save
2. On GitHub project repo > Settings > Webhooks > Add webhook 
3. But our Jenkins container is running locally thus GitHub can't locate its endpoint on the cloud. 
4. Since, I am not using a cloud VM, I will expose my Jenkins container to the cloud using NGROK bcuz Payload URL: the jenkins url (http://ip:8080/github-webhook/) will NOT work.
5. Install NGROK on your linux system (Ngrok creates a secure tunnel)
```sh
sudo snap install ngrok
```
6. Go to https://dashboard.ngrok.com/signup and sign up. Save your ngrok recovery files safe & setup MFA as well.
7. On your linux terminal, run the command that's given to add authtoken to the default ngrok.yml config file. The command looks like this: 
```sh
ngrok config add-authtoken someRandomStringHere
```
8. Now, Deploy your app online with the command
```sh
ngrok http http://ip:8080
```
9. You will get a public URL (forwarding link) that looks like 
```sh
https://582f-2603-8080-91f0-4ef0-e8ad-7ee-7ee8-4a4f.ngrok-free.app -> http://ip:8080
```

Now your payload URL should be:
```sh
Payload URL: the jenkins url (https://582f-2603-8080-91f0-4ef0-e8ad-7ee-7ee8-4a4f.ngrok-free.app/github-webhook/)
Content type: application/json
Secret: leave blank
SSL verification: Disable
Even: Just the push event
Active
Add webhook
```


## Clean Up
1. Stop and Delete the ACI service
2. Delete the ACR Repo and Image
3. Stop and Delete Jenkins & SonarQube containers


# LIKE, COMMENT, & SUBSCRIBE To iQuant on YouTube !!!
