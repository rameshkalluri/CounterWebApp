pipeline {
    agent any

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
                bat 'docker tag  cw:latest rrksrb/cw:${env.BUILD_NUMBER}'
            }
        }      
        stage('publish to registry') {
            steps{
                withDockerRegistry(credentialsId: 'docker', url: 'https://index.docker.io/v1/') {
                   bat 'docker push rrksrb/cw:${env.BUILD_NUMBER}'
                }
            }
        }
    }
}
