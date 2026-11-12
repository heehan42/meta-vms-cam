SUMMARY = "USB 카메라 영상을 H.264로 인코딩해 RTSP로 제공하는 카메라 서버"
HOMEPAGE = "https://github.com/heehan42/vms-cam"

# vms-cam 저장소에는 라이선스 파일이 없다. CLOSED이면 LIC_FILES_CHKSUM 검사를 하지 않는다.
LICENSE = "CLOSED"

# SRCREV는 원격 저장소에 push된 커밋만 가리킬 수 있다. 소스를 바꾸려면 vms-cam의 커밋을 먼저 push한다.
SRC_URI = " \
    git://github.com/heehan42/vms-cam.git;protocol=https;branch=main \
    file://vms-camera-server.init \
    file://vms-camera-server.default \
"
SRCREV = "74beff78894869f3dd1c074adc244a73a7abc378"
PV = "0.1+git"

inherit cmake pkgconfig update-rc.d

DEPENDS = "gstreamer1.0 gstreamer1.0-plugins-base gstreamer1.0-rtsp-server"

# ARM용 테스트 바이너리는 빌드 호스트에서 실행할 수 없으므로 테스트를 빌드하지 않는다.
EXTRA_OECMAKE = "-DVMS_CAM_BUILD_TESTS=OFF"

# 앱은 GStreamer 요소를 이름으로 찾으므로, 링크 정보로는 플러그인 의존성이 드러나지 않는다.
# 그래서 앱이 쓰는 요소의 플러그인 패키지를 직접 적는다. rtpbin, udpsink, udpsrc, appsrc, appsink는 gst-rtsp-server가 쓴다.
# gst-rtsp-server는 RTSP over TCP로 RTP를 보낼 때 appsrc와 appsink를 쓴다.
RDEPENDS:${PN} = " \
    gstreamer1.0-plugins-good-video4linux2 \
    gstreamer1.0-plugins-good-jpeg \
    gstreamer1.0-plugins-good-rtp \
    gstreamer1.0-plugins-good-rtpmanager \
    gstreamer1.0-plugins-good-udp \
    gstreamer1.0-plugins-base-app \
    gstreamer1.0-plugins-base-videoconvertscale \
    gstreamer1.0-plugins-base-videotestsrc \
    gstreamer1.0-plugins-bad-videoparsersbad \
    gstreamer1.0-plugins-ugly-x264 \
"

# 시작 순서 90은 syslog, 네트워크, eudev보다 뒤다. 그래서 서버가 시작할 때 syslog와 네트워크가 준비되어 있다.
INITSCRIPT_NAME = "vms-camera-server"
INITSCRIPT_PARAMS = "defaults 90 10"

# 사용자가 고친 서버 인자가 패키지를 다시 설치해도 유지되도록 설정 파일로 등록한다.
CONFFILES:${PN} = "${sysconfdir}/default/vms-camera-server"

# vms-cam의 CMakeLists.txt에는 install 규칙이 없다. 그래서 빌드한 실행 파일을 직접 설치한다.
do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/vms_camera_server ${D}${bindir}/vms_camera_server

    install -d ${D}${sysconfdir}/init.d ${D}${sysconfdir}/default
    install -m 0755 ${UNPACKDIR}/vms-camera-server.init ${D}${sysconfdir}/init.d/vms-camera-server
    install -m 0644 ${UNPACKDIR}/vms-camera-server.default ${D}${sysconfdir}/default/vms-camera-server
}
