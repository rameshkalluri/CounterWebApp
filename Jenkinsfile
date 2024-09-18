pipeline {
    agent any
    environment {
        DOCKER_IMAGE = 'rrksrb/cw'
        IMAGE_TAG = "${env.BUILD_NUMBER}" // Correctly use env.BUILD_NUMBER
    }
    stages {
        stage('checkout') {
            steps {
                git 'https://github.com/rameshkalluri/CounterWebApp.git'
            }
        }
        stage('build') {
            steps {
                bat 'mvn clean install'
            }
        }
        stage('Docker build') {
            steps {
                bat 'docker build -t cw .'
            }
        }      
        stage('Docker tag') {
            steps {
                bat "docker tag cw:latest ${DOCKER_IMAGE}:${IMAGE_TAG}"
            }
        }      
        stage('publish to registry') {
            steps{
                withDockerRegistry(credentialsId: 'docker', url: 'https://index.docker.io/v1/') {
                    bat "docker push ${DOCKER_IMAGE}:${IMAGE_TAG}"
                }
            }
        }
    }
}
