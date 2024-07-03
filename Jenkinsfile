pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git 'https://github.com/rameshkalluri/CounterWebApp.git'
            }
        }
        stage("maven Build"){
            steps{
                if(isUnix()) {
                    sh " mvn clean install"
                }else {
                     bat '''set MAVEN_HOME=C:\\ProgramData\\chocolatey\\lib\\maven\\apache-maven-3.9.8
                    set PATH=%MAVEN_HOME%\\bin;%PATH%
                    mvn clean install'''
                }
            }
        }
        stage("deploy to container"){
            steps{
                deploy adapters: [tomcat9(credentialsId: 'tomcatadmin', path: '', url: 'http://3.208.9.238:8080/')], contextPath: null, war: '**/*.war'
            }
        }
    }
}
