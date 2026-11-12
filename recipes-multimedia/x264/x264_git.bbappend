# ffmpeg 옵션은 x264 명령행 도구가 동영상 파일을 입력으로 읽을 때만 쓴다. GStreamer의 x264enc는 libx264만 쓴다.
# ffmpeg 레시피도 commercial license flag를 가지므로, 이 옵션을 빼서 허용할 commercial 레시피를 x264와 gstreamer1.0-plugins-ugly로 한정한다.
PACKAGECONFIG:remove = "ffmpeg"

# x264의 ARM 어셈블리 파일에는 .note.GNU-stack 섹션이 없다. 그래서 링커가 libx264.so에 실행 가능 스택을 요구하는 표시를 붙인다.
# glibc 2.41부터 dlopen은 이 표시가 있는 라이브러리를 거부한다. 그러면 GStreamer가 x264enc 플러그인을 로드하지 못한다.
# x264 코드는 스택에서 코드를 실행하지 않으므로, 링크할 때 실행 불가 스택을 지정한다.
LDFLAGS:append = " -Wl,-z,noexecstack"
