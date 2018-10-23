pipeline {
  agent any
  stages {
    stage('build') {
      steps {
        sh 'mvn clean install'
      }
    }
    stage('copy arf') {
      steps {
        sh '''cp -rf /var/lib/jenkins/workspace/CounterWebApp_master-P3FIAMTBSDJCWGWTEFF5324Y6XXFO2VJW3HNC2IALQNNQ4BNBA4A/target/CounterWebApp.war  /var/lib/jenkins/
'''
      }
    }
    stage('error') {
      parallel {
        stage('error') {
          steps {
            git(url: 'https://github.com/rameshkalluri/CentOS-Dockerfiles.git', branch: 'master', poll: true)
          }
        }
        stage('error') {
          steps {
            git(url: 'https://rameshkalluri:Kramesh@143@bitbucket.org/pragiti-git/hymetals-hybris-extensions.git', branch: 'master', poll: true)
          }
        }
      }
    }
  }
}