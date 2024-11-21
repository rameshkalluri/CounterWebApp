pipeline {
      agent { 
        label 'aws'
    }
    
    stages {
        stage("Maven Build") {
            steps{
                sh 'mvn clean install'
            }
        }  
        stage("tomcat deployment") {
            steps{
                deploy adapters: [tomcat9(credentialsId: 'tomcat', path: '', url: 'http://15.206.116.148:8090')], contextPath: 'hari', war: '**/*.war'
            }
        }    
    }
}
