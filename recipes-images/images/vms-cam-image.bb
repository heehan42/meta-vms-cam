SUMMARY = "부팅할 때 vms_camera_server를 시작하는 카메라 장비 이미지"

require recipes-core/images/core-image-minimal.bb

# kernel-module-uvcvideo는 core-image-minimal에도 들어 있다.
# 그래도 직접 적어서, USB 카메라 인식이 다른 레시피의 설정에 기대지 않게 한다.
IMAGE_INSTALL:append = " vms-camera-server kernel-module-uvcvideo"

# 검증할 때 시리얼 콘솔 없이 /var/log/messages를 읽기 위해 SSH 서버를 넣는다. root 로그인 허용 여부는 빌드 설정이 정한다.
IMAGE_FEATURES:append = " ssh-server-openssh"
