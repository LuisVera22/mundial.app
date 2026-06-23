pipeline {
    agent any

    tools {
        jdk 'java-jdk-21'
        maven 'maven-3.9'
    }

    options {
        timestamps()
        timeout(time: 20, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/LuisVera22/mundial.app.git'
            }
        }

        stage('Compilar y testear') {
            steps {
                sh 'mvn -B -ntp clean verify'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Archivar artefacto') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint:true, onlyIfSuccessful: true
            }
        }

        post {
            failure { echo 'Build falló'}
        }
    }
}