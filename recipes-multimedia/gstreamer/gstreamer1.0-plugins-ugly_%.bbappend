# vms_camera_server는 x264enc 요소로 H.264를 인코딩한다. 레시피의 기본 PACKAGECONFIG에는 x264가 없어서 더한다.
# 이 레시피와 x264 레시피는 commercial license flag를 가지므로 vms-cam/x264-license fragment를 켜야 빌드된다.
PACKAGECONFIG:append = " x264"
