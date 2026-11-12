# meta-vms-cam

`meta-vms-cam`은 BeagleBone Black(BBB)을 카메라 장비로 쓰는 Yocto 레이어다. 이 레이어는 [vms-cam](https://github.com/heehan42/vms-cam) 저장소의 `vms_camera_server`를 BBB용으로 빌드한다. 그다음 부팅할 때 카메라 서버가 시작하는 이미지를 만든다. 카메라 서버는 USB 카메라 영상을 H.264로 인코딩해 RTSP로 제공한다.

## 빌드 환경

이 문서는 다음 빌드 환경을 기준으로 한다.

- Yocto 6.0(wrynose). 빌드는 bitbake-setup으로 만든다. bitbake-setup은 Yocto 빌드 디렉터리를 만들고, 설정 파일에 적힌 레이어 저장소를 받아 오는 도구다.
- 빌드의 최상위 디렉터리(setup 디렉터리)는 `~/yocto/bitbake-builds/poky-wrynose`다. 아래 명령의 상대 경로는 이 디렉터리를 기준으로 한다.
- 레이어는 `layers/` 아래에 있다. 빌드 디렉터리는 `build/`다.
- 빌드 설정은 `MACHINE = "beaglebone"`이고, BSP 레이어는 meta-ti(meta-ti-bsp, meta-beagle)다.

이 레이어는 oe-core에만 의존한다. 레시피와 이미지에 BBB 전용 설정이 없기 때문이다. BBB 전용 값은 빌드 설정의 `MACHINE`뿐이다.

## 레이어 추가

`meta-vms-cam`은 bitbake-setup이 관리하지 않는다. 그래서 사용자가 이 저장소를 `layers/`에 직접 clone한다.

```bash
cd ~/yocto/bitbake-builds/poky-wrynose
git clone https://github.com/heehan42/meta-vms-cam.git layers/meta-vms-cam
```

그다음 빌드 환경을 설정하고 레이어를 `build/conf/bblayers.conf`에 추가한다.

```bash
cd ~/yocto/bitbake-builds/poky-wrynose/build
. ./init-build-env
bitbake-layers add-layer ../layers/meta-vms-cam
```

`bitbake-layers show-layers`의 출력에 `vms-cam` 레이어가 있으면 추가된 것이다.

```text
vms-cam               /home/<사용자>/yocto/bitbake-builds/poky-wrynose/layers/meta-vms-cam      6
```

## `bitbake-setup update`와 `bblayers.conf`

`bitbake-setup update`는 bitbake-setup 설정 파일에 적힌 레이어 저장소를 다시 받는다. 이때 bitbake 설정 파일의 갱신에 동의하면, bitbake-setup이 `bblayers.conf`를 설정 파일 기준으로 다시 만든다. 설정 파일에는 `meta-vms-cam`이 없다. 그래서 다시 만든 `bblayers.conf`에서 `meta-vms-cam`이 빠진다.

이 문제를 피하려면 `--update-bb-conf no`로 bitbake 설정 파일의 갱신을 거절한다.

```bash
cd ~/yocto/bitbake-builds/poky-wrynose
bitbake-setup update --setup-dir . --update-bb-conf no
```

`bblayers.conf`에서 `meta-vms-cam`이 이미 빠졌다면, [레이어 추가](#레이어-추가)의 `bitbake-layers add-layer` 명령을 다시 실행한다. `layers/meta-vms-cam` 디렉터리는 bitbake-setup이 지우지 않는다. 그래서 다시 clone하지 않아도 된다.

## x264 라이선스 fragment

`vms_camera_server`는 GStreamer의 `x264enc` 요소로 영상을 H.264로 인코딩한다. 이 요소는 `x264` 레시피와 `gstreamer1.0-plugins-ugly` 레시피가 만든다. 두 레시피에는 `LICENSE_FLAGS = "commercial"`이 붙어 있다. `LICENSE_FLAGS`는 레시피의 사용 제약 표시다. H.264 코덱이 특허로 보호되므로 두 레시피가 `commercial` flag를 갖는다. 빌드 설정의 `LICENSE_FLAGS_ACCEPTED`가 flag를 허용하지 않으면, BitBake는 이 레시피를 빌드하지 않는다.

이 레이어는 두 레시피의 flag만 허용하는 config fragment `vms-cam/x264-license`를 제공한다. config fragment는 레이어의 `conf/fragments/` 아래에 있는 작은 설정 파일이다. fragment를 켜면 BitBake가 설정을 읽을 때 이 파일을 함께 읽는다. 이 fragment는 `commercial` 전체를 허용하지 않는다. 그래서 다른 commercial 레시피가 의존성을 따라 이미지에 들어오면 빌드가 계속 막힌다.

fragment는 다음 명령으로 켠다.

```bash
cd ~/yocto/bitbake-builds/poky-wrynose/build
. ./init-build-env
bitbake-config-build enable-fragment vms-cam/x264-license
```

이 명령은 `build/conf/toolcfg.conf`의 `OE_FRAGMENTS`에 `vms-cam/x264-license`를 기록한다. 켜졌는지는 다음 명령으로 확인한다.

```bash
bitbake -e x264 | grep ^LICENSE_FLAGS_ACCEPTED
```

출력에 `commercial_x264`와 `commercial_gstreamer1.0-plugins-ugly`가 있으면 켜진 것이다.

이 레이어는 두 레시피에 bbappend도 하나씩 둔다. bbappend는 다른 레이어의 레시피에 설정을 덧붙이는 파일이다.

- `gstreamer1.0-plugins-ugly_%.bbappend`: `PACKAGECONFIG`에 `x264`를 더한다. 이 레시피의 기본 설정은 x264 플러그인을 빌드하지 않기 때문이다.
- `x264_git.bbappend`: `PACKAGECONFIG`에서 `ffmpeg`를 뺀다. `ffmpeg` 레시피도 `commercial` flag를 갖기 때문이다. 이 기능은 x264 명령행 도구만 쓰고, `x264enc`는 쓰지 않는다.

## `vms-camera-server` 레시피

`recipes-multimedia/vms-camera-server/vms-camera-server_git.bb`는 vms-cam 저장소의 커밋 하나를 받아 `vms_camera_server`를 빌드한다.

- `SRC_URI`: `git://github.com/heehan42/vms-cam.git;protocol=https;branch=main`. 레시피는 이 주소에서 vms-cam 저장소의 `main` 브랜치를 받는다.
- `SRCREV`: 빌드할 vms-cam 커밋의 해시. 이 값을 고정하면 언제 빌드해도 같은 소스로 빌드한다.
- `EXTRA_OECMAKE`: `-DVMS_CAM_BUILD_TESTS=OFF`. 레시피는 테스트를 빌드하지 않는다. ARM용 테스트 바이너리는 빌드 호스트에서 실행할 수 없기 때문이다.
- `RDEPENDS:${PN}`: `vms_camera_server`가 쓰는 GStreamer 플러그인 패키지. 앱은 GStreamer 요소를 이름으로 찾는다. 그래서 링크 단계에서는 플러그인 의존성이 드러나지 않는다. 이 변수에 플러그인 패키지를 직접 적으면, `vms-camera-server`를 설치할 때 플러그인도 함께 설치된다.

### `SRCREV` 바꾸기

`SRCREV`는 원격 저장소에 있는 커밋만 가리킬 수 있다. BitBake가 원격 저장소에서 소스를 받기 때문이다. 그래서 다음 순서로 바꾼다.

1. vms-cam의 변경을 커밋하고 `origin/main`에 push한다.
2. push한 커밋의 해시를 확인한다.

    ```bash
    git -C ~/vms-cam ls-remote origin main
    ```

3. 레시피의 `SRCREV`를 이 해시로 바꾼다.
4. `bitbake vms-camera-server`로 빌드한다.

### 개발 중 빌드

개발 중에는 커밋과 push 없이 `~/vms-cam` 작업 트리를 BBB용으로 빌드한다. 이때 devtool을 쓴다. devtool은 레시피의 소스를 로컬 작업 트리로 바꿔 빌드하게 해 주는 도구다.

```bash
cd ~/yocto/bitbake-builds/poky-wrynose/build
. ./init-build-env
devtool modify -n vms-camera-server ~/vms-cam
```

`-n`(`--no-extract`)은 이미 있는 `~/vms-cam` 작업 트리를 그대로 쓰게 하는 옵션이다. 이 옵션이 없으면 devtool은 `output path ... already exists and is non-empty` 오류로 실패한다.

이 명령은 `build/workspace/appends/`에 bbappend를 만든다. 이 bbappend는 레시피의 소스 위치를 `SRC_URI`에서 `~/vms-cam`으로 바꾼다. 그래서 `bitbake vms-camera-server`가 `~/vms-cam`의 커밋하지 않은 변경까지 빌드한다. 빌드 결과 바이너리는 `build/tmp/work/armv7at2hf-neon-poky-linux-gnueabi/vms-camera-server/0.1+git/image/usr/bin/vms_camera_server`에 생긴다.

devtool은 `~/vms-cam`에 링크 두 개(`oe-workdir`, `oe-logs`)도 만든다. 두 링크는 빌드 디렉터리의 작업 디렉터리와 로그 디렉터리를 가리킨다. devtool은 두 링크를 `.git/info/exclude`에 등록한다. 그래서 git은 두 링크를 변경으로 보지 않는다.

개발이 끝나면 bbappend를 지운다.

```bash
devtool reset vms-camera-server
```

`devtool reset`은 `~/vms-cam`의 두 링크를 지우지 않는다. 필요 없으면 직접 지운다.

```bash
rm ~/vms-cam/oe-workdir ~/vms-cam/oe-logs
```

그다음 [`SRCREV` 바꾸기](#srcrev-바꾸기)의 순서로 레시피를 새 커밋에 맞춘다.

## 이미지 빌드

`recipes-images/images/vms-cam-image.bb`는 `core-image-minimal`에 다음 항목을 더한 이미지다.

- `vms-camera-server`: 카메라 서버와 init 스크립트, 설정 파일. 플러그인 패키지는 이 패키지의 `RDEPENDS`를 따라 함께 들어간다.
- `kernel-module-uvcvideo`: USB UVC 카메라 드라이버
- `ssh-server-openssh`: SSH 서버. 시리얼 콘솔 없이 BBB에 로그인하기 위해 넣는다.

[레이어 추가](#레이어-추가)와 [x264 라이선스 fragment](#x264-라이선스-fragment)를 마친 뒤 이미지를 빌드한다.

```bash
cd ~/yocto/bitbake-builds/poky-wrynose/build
. ./init-build-env
bitbake vms-cam-image
```

빌드 결과는 `build/deploy-ti/images/beaglebone/`에 생긴다. SD 카드에 기록할 파일은 다음 두 개다.

- `vms-cam-image-beaglebone.rootfs.wic.xz`: 파티션까지 나뉜 디스크 이미지(wic)를 xz로 압축한 파일
- `vms-cam-image-beaglebone.rootfs.wic.bmap`: 디스크 이미지에서 데이터가 있는 블록의 목록

fragment를 켜지 않으면 빌드가 실패한다. 이때 오류 메시지는 license flag를 직접 말하지 않는다. 대신 빌드할 수 없는 의존성 사슬에 `gstreamer1.0-plugins-ugly-x264`가 나온다.

```text
ERROR: Nothing RPROVIDES 'gstreamer1.0-plugins-ugly-x264' (but .../vms-camera-server_git.bb RDEPENDS on or otherwise requires it)
```

## SD 카드 기록

이 절은 WSL2에서 USB 카드 리더로 SD 카드에 기록하는 방법을 설명한다. 카드 리더는 usbipd-win으로 WSL2에 연결한다. usbipd-win은 Windows의 USB 장치를 WSL2에 넘겨 주는 도구다. 노트북 내장 SD 슬롯은 대부분 PCIe 장치다. 그래서 내장 슬롯은 `usbipd list`에 나오지 않고, 이 방법으로 쓸 수 없다.

1. WSL에 bmaptool을 설치한다. bmaptool은 bmap 파일에 적힌 블록만 기록하고, 기록한 뒤 체크섬을 검사한다.

    ```bash
    sudo apt install -y bmap-tools
    ```

2. SD 카드를 카드 리더에 꽂는다. 그다음 관리자 PowerShell에서 카드 리더의 BUSID를 찾는다. 카드 리더는 보통 "USB 대용량 저장 장치"로 나온다.

    ```powershell
    usbipd list
    ```

3. 카드 리더를 WSL2에 연결한다. `bind`는 처음 한 번만 한다.

    ```powershell
    usbipd bind --busid <BUSID>
    usbipd attach --wsl --busid <BUSID>
    ```

4. WSL에서 SD 카드의 장치 이름을 찾는다. `TRAN`이 `usb`이고 크기가 SD 카드와 같은 장치가 SD 카드다. `TRAN`이 비어 있는 `Virtual Disk`는 WSL의 가상 디스크이므로 고르지 않는다.

    ```bash
    sudo modprobe usb-storage
    lsblk -d -o NAME,SIZE,TRAN,MODEL
    ```

5. 이미지를 기록한다. `/dev/sdX`는 4단계에서 찾은 장치로 바꾼다. 파티션(`/dev/sdX1`)이 아니라 장치 전체를 쓴다. bmaptool은 같은 디렉터리의 `.wic.bmap` 파일을 자동으로 찾는다.

    ```bash
    cd ~/yocto/bitbake-builds/poky-wrynose/build/deploy-ti/images/beaglebone
    sudo bmaptool copy vms-cam-image-beaglebone.rootfs.wic.xz /dev/sdX
    sync
    ```

    출력 끝에 `bmaptool: info: copying time: ...` 줄이 나오면 기록이 끝난 것이다.

6. 관리자 PowerShell에서 카드 리더의 연결을 해제한다. 그다음 SD 카드를 뺀다.

    ```powershell
    usbipd detach --busid <BUSID>
    ```

## SD 카드로 부팅

BBB는 기본으로 eMMC에서 부팅한다. SD 카드로 부팅하려면 전원을 넣을 때 S2 버튼을 누르고 있어야 한다. S2 버튼은 SD 카드 슬롯 근처에 있다.

1. BBB의 전원을 뺀 상태에서 SD 카드를 꽂는다. 유선 이더넷도 연결한다.
2. S2 버튼을 누른 채 전원을 넣는다.
3. 전원 LED가 켜지고 몇 초 뒤 S2 버튼을 놓는다.

커널이 부팅하면 USR0 LED가 두 번씩 깜박이기를 반복한다.

## 로그인

빌드 설정에서 `core/yocto/root-login-with-empty-password` fragment를 켰다면, `root`로 비밀번호 없이 로그인할 수 있다.

### 시리얼 콘솔

BBB의 시리얼 콘솔은 6핀 헤더 J1에 있다. J1은 이더넷 포트 반대쪽 가장자리에 있고, 1번 핀에 흰 점이 있다. USB-시리얼 변환기(3.3 V)를 다음과 같이 연결한다. 변환기의 5 V 선은 연결하지 않는다.

| J1 핀 | 신호 | 변환기의 선 |
| --- | --- | --- |
| 1 | GND | GND |
| 4 | RX (BBB가 받음) | TX |
| 5 | TX (BBB가 보냄) | RX |

Windows에서는 PuTTY로 접속한다. 설정은 다음과 같다.

- Connection type: Serial
- Serial line: 장치 관리자의 "포트(COM & LPT)"에 나오는 변환기의 COM 포트
- Speed: 115200
- Connection > Serial: Data bits 8, Stop bits 1, Parity None, Flow control None

변환기를 usbipd로 WSL2에 연결했다면 Windows에서 COM 포트를 열 수 없다. 이때는 `usbipd detach`로 연결을 해제한다.

### SSH

공유기의 DHCP 목록에서 BBB의 주소를 확인한다. 그다음 다음 명령으로 접속한다.

```bash
ssh root@<BBB 주소>
```

## `vms-camera-server` 서비스

`vms-camera-server` 서비스는 `vms_camera_server` 실행 파일을 백그라운드에서 실행하는 sysvinit 서비스다. 이미지는 부팅할 때 이 서비스를 시작 순서 90으로 시작한다. 시작 순서 90은 syslog와 네트워크보다 뒤다.

### 서비스 제어

서비스는 `/etc/init.d/vms-camera-server` 스크립트로 제어한다. 이 스크립트는 다음 세 명령을 받는다.

| 명령 | 동작 | 출력 예 |
| --- | --- | --- |
| `start` | 서버를 시작한다. 서버가 이미 실행 중이면 아무것도 하지 않는다. | `vms-camera-server: 시작했습니다 (PID 405)` |
| `stop` | 서버에 `SIGTERM`을 보낸다. 그다음 서버가 끝날 때까지 최대 10초 기다린다. | `vms-camera-server: 중지했습니다` |
| `restart` | `stop`을 실행한다. 그다음 `start`를 실행한다. | `중지했습니다`와 `시작했습니다`가 차례로 나온다 |

```bash
/etc/init.d/vms-camera-server restart
```

서버는 시작 직후 카메라 장치를 확인한다. 장치를 열 수 없으면 서버는 바로 종료한다. 그래서 `start`는 서버를 실행한 뒤 3초 기다린다. 그다음 서버가 아직 실행 중인지 확인한다. 서버가 종료되었으면 `start`는 다음 줄을 출력하고 종료 코드 1을 반환한다. 이때 원인은 [syslog](#syslog)에서 확인한다.

```text
vms-camera-server: 시작 직후 종료되었습니다. 원인은 /var/log/messages에서 확인하세요
```

서버의 PID는 `/var/run/vms-camera-server.pid`에 기록된다. 서버가 실행 중인지는 `ps`로 확인한다.

```bash
ps | grep [v]ms_camera_server
```

이미지에는 `netstat`과 `ss`가 없다. 그래서 RTSP 포트 8554의 LISTEN 여부는 `/proc/net/tcp`에서 확인한다. 이 파일은 포트를 16진수로 적는다. 포트 8554는 `216A`이고, LISTEN 상태는 `0A`다. 다음 명령이 1을 출력하면 서버가 포트 8554에서 접속을 기다리고 있다.

```bash
grep -c ':216A [0-9A-F:]* 0A' /proc/net/tcp
```

### 설정 파일

`/etc/default/vms-camera-server`는 서버에 넘길 명령행 인자를 `VMS_CAMERA_SERVER_ARGS` 변수에 담는다. 기본값은 다음과 같다.

```sh
VMS_CAMERA_SERVER_ARGS="--port 8554 --mount /cam"
```

인자를 적지 않은 해상도와 fps에는 앱의 기본값(640x480, 30 fps)이 쓰인다. 예를 들어 320x240, 15 fps로 바꾸려면 다음 순서를 따른다.

1. 설정 파일의 값을 `"--port 8554 --mount /cam --width 320 --height 240 --fps 15"`로 고친다.
2. `/etc/init.d/vms-camera-server restart`를 실행한다.
3. syslog에서 `해상도 320x240, 15 fps`로 시작하는 줄을 확인한다.

이 파일은 패키지의 설정 파일(`CONFFILES`)로 등록되어 있다. 그래서 패키지를 다시 설치해도 고친 값이 유지된다.

### syslog

서버의 표준 출력과 표준 오류는 `logger`를 거쳐 syslog에 `vms-camera-server` 태그로 기록된다. BusyBox `syslogd`는 이 기록을 `/var/log/messages` 파일에 쓴다. 이 설정에서는 BusyBox `logread`가 동작하지 않는다. 그래서 기록은 파일에서 직접 읽는다.

```bash
grep vms-camera-server /var/log/messages
```

서버가 시작되면 `RTSP 서버 준비 완료` 줄과 해상도·fps 설정 줄이 기록된다. 클라이언트가 접속해 있는 동안에는 `인코딩 통계` 줄이 5초마다 기록된다. 서버가 시작할 때 카메라를 열 수 없으면 `장치를 열 수 없습니다` 줄이 기록된다. 그다음 서버는 종료한다.

`/var/log/messages`가 200 KB를 넘으면 `syslogd`가 이 파일을 `messages.0`으로 옮긴다. 그다음 `syslogd`는 새 파일에 기록한다. 또 `/var/log`는 tmpfs다. 그래서 재부팅하면 기록이 사라진다.

## VMS 서버 연동

이 절은 WSL2에서 실행하는 VMS 서버(`vms` 저장소의 `vms_web_server`)가 BBB의 카메라 영상을 받는 방법을 설명한다. 이 절의 명령은 BBB의 주소가 `192.168.219.106`이라고 가정한다.

### 준비: Hyper-V 방화벽

VMS 서버는 RTSP 제어를 TCP로 보낸다. 그리고 영상(RTP)은 UDP로 받는다. 이때 RTP를 받는 포트는 VMS 서버가 연결할 때마다 커널에게 받는 임시 포트다. 그래서 포트 번호를 미리 정할 수 없다.

WSL2가 mirrored 네트워크 모드이면, LAN에서 WSL2로 들어오는 패킷에 Hyper-V 방화벽이 적용된다. Hyper-V 방화벽의 기본 동작은 들어오는 패킷을 막는 것이다. 이 동작은 같은 LAN의 장비가 보낸 패킷에도 적용된다. RTSP 제어 연결은 VMS 서버가 먼저 열었으므로 통과한다. 하지만 RTP는 BBB가 먼저 보내는 UDP다. 그래서 허용 규칙이 없으면 RTP가 막힌다. 이때 VMS 서버는 연결에 성공한 뒤 3초마다 `카메라 끊김: bbb: RTP가 3000ms 동안 없음`을 출력한다.

WSL2의 Hyper-V 방화벽 설정은 Windows PowerShell에서 다음 명령으로 확인한다. `{40E0AC32-46A5-438A-A0B2-2B479E8F2E90}`은 WSL을 가리키는 고정 ID다.

```powershell
Get-NetFirewallHyperVVMSetting -PolicyStore ActiveStore -Name '{40E0AC32-46A5-438A-A0B2-2B479E8F2E90}'
Get-NetFirewallHyperVRule -VMCreatorId '{40E0AC32-46A5-438A-A0B2-2B479E8F2E90}'
```

첫 명령의 `DefaultInboundAction`이 `Block`이고, 두 번째 명령의 결과에 BBB의 UDP를 허용하는 규칙이 없으면 규칙을 추가한다. 규칙은 관리자 권한 PowerShell에서 추가한다. 다음 규칙은 BBB 주소에서 오는 UDP만 허용한다.

```powershell
New-NetFirewallHyperVRule -Name vms-cam-rtp -DisplayName "vms-cam RTP from BBB" -Direction Inbound -VMCreatorId '{40E0AC32-46A5-438A-A0B2-2B479E8F2E90}' -Protocol UDP -RemoteAddresses 192.168.219.106 -Action Allow
```

규칙을 지우려면 `Remove-NetFirewallHyperVRule -Name vms-cam-rtp`를 실행한다.

### VMS 서버 실행

VMS 서버를 빌드하는 방법은 `vms` 저장소의 README를 본다. `vms` 저장소의 루트에서 다음 명령으로 VMS 서버를 실행한다. 이 명령은 BBB의 RTSP 주소를 카메라 이름 `bbb`로 등록한다.

```bash
./build/vms_web_server --ice-address 192.168.219.105 --ice-address 127.0.0.1 --camera bbb=rtsp://192.168.219.106:8554/cam
```

`192.168.219.105`는 WSL2를 실행하는 PC의 LAN 주소다. 실제 주소는 WSL2에서 `ip -4 addr`로 확인한다. `--ice-address 127.0.0.1`은 같은 PC의 Windows 브라우저가 연결할 때 필요하다. mirrored 모드에서는 Windows가 자기 LAN 주소로 보낸 UDP가 WSL2로 전달되지 않기 때문이다. 이 인자가 없으면 브라우저의 ICE 상태가 `checking`에서 `disconnected`로 바뀌고, 연결 상태는 `failed`가 된다.

VMS 서버가 BBB에 연결되면 다음 줄을 출력한다.

```text
카메라 연결 성공: bbb, 해상도 640x480, payload type 96, 세션 timeout 60초
녹화 시작: bbb
```

연결된 뒤에는 `카메라 끊김`이 출력되지 않아야 한다. 그리고 BBB의 syslog에 `인코딩 통계` 줄이 5초마다 기록된다. 이 줄은 BBB에서 다음 명령으로 확인한다.

```bash
grep '인코딩 통계' /var/log/messages | tail -n 3
```

### 브라우저 재생

1. Windows 브라우저에서 `http://localhost:8080/`을 연다.
2. **소스** 목록에서 "카메라 · bbb"를 고른다.
3. 연결한다.

연결 상태가 `connected`가 되면 C270의 영상이 재생된다.

### 측정 결과

VMS 서버가 BBB의 영상을 받는 동안 두 설정을 각각 1분씩 측정했다. 측정 조건은 다음과 같다.

- BBB: Cortex-A8 1 GHz 1코어, 이미지의 기본 x264 설정(`speed-preset=ultrafast`, `tune=zerolatency`, 목표 비트레이트 2000 kbps)
- fps와 비트레이트: BBB syslog의 `인코딩 통계` 줄 12개(5초 간격)
- CPU 사용률: BBB에서 10초 간격으로 실행한 `top -b -n 1`의 결과 6개
- 패킷 손실: 1분 동안 VMS 서버가 출력한 `녹화 프레임 버림: bbb (패킷 손실)` 줄의 수

| 설정 | 인코딩 fps | 비트레이트 | `vms_camera_server` CPU | 전체 CPU idle | 패킷 손실 |
| --- | --- | --- | --- | --- | --- |
| 640x480, 30 fps (기본값) | 13.4–16.6 | 920–1130 kbps | 77–93% | 0% | 1회 |
| 320x240, 15 fps | 15.0 | 1980–2010 kbps | 31–50% | 41–50% | 2회 |

기본 설정에서는 BBB의 CPU가 포화된다. 그래서 인코딩 fps가 목표인 30 fps의 절반 정도에 그친다. 또 인코더가 프레임을 덜 만들므로, 비트레이트도 목표인 2000 kbps보다 낮다.

320x240, 15 fps에서는 목표 fps와 목표 비트레이트를 모두 지킨다. 이때 CPU는 절반 정도 남는다.

두 설정 모두 1분에 한두 번 패킷 손실이 있었다. 패킷이 빠지면 브라우저의 화면은 다음 키프레임까지 멈췄다가 이어진다. 이 측정만으로는 손실의 원인을 정하지 않았다.
