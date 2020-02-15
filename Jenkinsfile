#!/usr/bin/env groovy
/* REQUIRED CREDENTTIALS
 *
 * - poterion-git               [SSH Key]   (global)
 */

def setup() {
    if (env.VERSION) {
        echo "Setup already done."
    } else {
        def major = sh(returnStdout: true, script: 'date +%Y').replaceAll(/\s/, "")
        def minor
        if (BRANCH_NAME == 'master' || BRANCH_NAME ==~ /release\/.*/) {
            minor = sh(returnStdout: true, script: "git tag | grep -E '^v${major}\\.[0-9]+' | sort -r | head -n 1 | sed -E 's/^v${major}\\.([0-9]+)\$/\\1/'")
            if (minor == "") minor = "0"
            minor = minor.toInteger() + 1
        } else if (BRANCH_NAME ==~ /\w+\/.*/) {
            minor = BRANCH_NAME.replaceAll(/\w+\/(.*)/) { matches -> "${matches[1]}" }
        } else {
            minor = "DEV"
        }
        env.VERSION = major + '.' + minor
        currentBuild.displayName = "${VERSION}.${BUILD_NUMBER}"
        echo "Building version: ${VERSION}"
    }
}

node {
    //agent any
    env.JAVA_HOME = "${tool name: 'JDK_8'}"
    env.GRADLE = "${tool 'Gradle 5.2.1'}"
    env.PATH = "${env.GRADLE}/bin:${env.PATH}"

    properties([
            //timeout(time: 1, unit: 'HOURS'),
            disableConcurrentBuilds(),
            buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '20'))
    ])

    timestamps {
        ansiColor('xterm') {
            stage('Checkout') {
                if (BRANCH_NAME ==~ /release\/.*/) cleanWs()

                checkout([
                        $class                           : 'GitSCM',
                        poll                             : true,
                        branches                         : [[name: "*/${BRANCH_NAME}"]],
                        doGenerateSubmoduleConfigurations: false,
                        extensions                       : [
                                [
                                        $class      : 'CloneOption',
                                        depth       : 1,
                                        honorRefspec: true,
                                        noTags      : false,
                                        reference   : '',
                                        shallow     : false
                                ],
                                [
                                        $class: 'SubmoduleOption',
                                        disableSubmodules: false,
                                        parentCredentials: true,
                                        recursiveSubmodules: true,
                                        reference: '',
                                        trackingSubmodules: false
                                ],
                                [$class: 'PruneStaleBranch'],
                                [$class: 'WipeWorkspace']//,
                                //[$class: 'CleanBeforeCheckout']//,
                                //[$class: 'CleanCheckout']
                        ],
                        submoduleCfg                     : [],
                        userRemoteConfigs                : [
                                [
                                        credentialsId: 'poterion-git',
                                        url          : 'ssh://git@bitbucket.intra:22999/potpho/application.git'
                                ]
                        ]
                ])

                sh 'git remote | grep github || git remote add github git@github.com:kubovy/footprint-photo-manager.git'
                sh 'git tag -l | xargs git tag -d'
                sshagent(credentials: ['poterion-git']) {
                    sh 'git fetch github'
                    sh 'git fetch github --tags'
                }
                setup()
                milestone(10)
            }

            stage('Prepare OpenJDK') {
                setup()
                sh "gradle -Pversion=${VERSION} -Pjdks=/tmp/jdks extractOpenJDK"
                milestone(20)
            }

            stage('Build') {
                setup()
                sh "gradle -Pversion=${VERSION} -Pjdks=/tmp/jdks extractOpenJDK build"
                milestone(30)
            }

            stage('Build FatJar') {
                setup()
                sh "gradle -Pversion=${VERSION} -Pjdks=/tmp/jdks extractOpenJDK assembleFatJar"
                archiveArtifacts([
                        fingerprint: true,
                        onlyIfSuccessful: true,
                        artifacts: "build/libs/footprint-all-${VERSION}.jar"
                ])
                milestone(40)
            }

            stage('Build Self-Contained (Linux)') {
                setup()
                sh "gradle -Pversion=${VERSION} -Pjdks=/tmp/jdks extractOpenJDK assembleSelfContained"
                archiveArtifacts([
                        fingerprint: true,
                        onlyIfSuccessful: true,
                        artifacts: "bundles/footprint-${VERSION}.tar.gz,"
                                + "bundles/footprint-${VERSION}.deb,"
                                + "bundles/footprint-${VERSION}.rpm"
                        ])
                milestone(50)
            }

            if (BRANCH_NAME == 'master') stage('Release') {
                setup()
                sshagent(credentials: ['poterion-git']) {
                    sh "git push github HEAD:${BRANCH_NAME} --force"
                }
                sh "rm -f dist/*.jar"
                sh "mkdir -p dist"
                sh "cp build/libs/footprint-all-${VERSION}.jar dist/footprint-all-${VERSION}.jar"
                sh "git config user.name 'Jenkins'"
                sh "git config user.email 'jenkins@poterion.com'"

                sh "git add dist/*.jar"
                sh "git commit -m \"Release ${VERSION}\""
                sh "git tag -a -f v${VERSION} -m \"Release ${VERSION}\""
                sh "git tag -a -f latest -m \"Release ${VERSION}\""
                sshagent(credentials: ['poterion-git']) {
                    sh "git push github --tags --force"
                }
                milestone(90)
            }
        }
    }
}