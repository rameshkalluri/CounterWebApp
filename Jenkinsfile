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
        stage('publish to registry') {
            steps{
                withDockerRegistry(credentialsId: 'docker', url: 'https://hub.docker.com/') {
                   bat 'docker tag cw:latest  rrksrb/cw:1
                        docker push rrksrb/cw:1
                    '
                }
            }
        }
    }
}
