pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
    }

    parameters {
        booleanParam(name: 'PUSH_IMAGES', defaultValue: false, description: 'Push images to registry')
        booleanParam(name: 'DEPLOY_COMPOSE', defaultValue: false, description: 'Run docker compose up -d')
        string(name: 'DOCKER_REGISTRY', defaultValue: 'docker.io', description: 'Docker registry host')
        string(name: 'DOCKER_NAMESPACE', defaultValue: 'smartpayhub', description: 'Image namespace or org')
        string(name: 'IMAGE_TAG', defaultValue: '', description: 'Image tag override (default: BUILD_NUMBER)')
    }

    environment {
        EFFECTIVE_TAG = "${params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER}"
        MODULES = "SPH-ApiGateway SPH-AuthService SPH-CloudConfigManagement SPH-MerchantService SPH-NotificationsService SPH-PaymentsService SPH-SettlementService SPH-TransactionHistory SPH-WalletService"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and Test') {
            steps {
                sh 'mvn -B clean verify'
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    def modules = env.MODULES.split(' ')
                    for (m in modules) {
                        def imageName = moduleToImage(m)
                        sh """
                            docker build \
                              --build-arg MODULE=${m} \
                              -t ${params.DOCKER_REGISTRY}/${params.DOCKER_NAMESPACE}/${imageName}:${env.EFFECTIVE_TAG} \
                              -t ${params.DOCKER_REGISTRY}/${params.DOCKER_NAMESPACE}/${imageName}:latest \
                              .
                        """
                    }
                }
            }
        }

        stage('Push Docker Images') {
            when {
                expression { params.PUSH_IMAGES }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin ${DOCKER_REGISTRY}'
                    script {
                        def modules = env.MODULES.split(' ')
                        for (m in modules) {
                            def imageName = moduleToImage(m)
                            sh """
                                docker push ${params.DOCKER_REGISTRY}/${params.DOCKER_NAMESPACE}/${imageName}:${env.EFFECTIVE_TAG}
                                docker push ${params.DOCKER_REGISTRY}/${params.DOCKER_NAMESPACE}/${imageName}:latest
                            """
                        }
                    }
                }
            }
        }

        stage('Deploy via Docker Compose') {
            when {
                expression { params.DEPLOY_COMPOSE }
            }
            steps {
                sh 'docker compose up -d --build'
            }
        }
    }

    post {
        always {
            sh 'docker logout ${DOCKER_REGISTRY} || true'
        }
    }
}

def moduleToImage(String module) {
    return module
        .replace('SPH-', 'sph-')
        .replace('Service', '-service')
        .replace('ConfigManagement', 'cloud-config')
        .replace('ApiGateway', 'api-gateway')
        .replace('TransactionHistory', 'transaction-history')
        .toLowerCase()
}
