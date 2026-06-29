; Smart Triage — Inno Setup 安装脚本
; 完全离线安装：内置 Python 运行时、PostgreSQL 便携版、Redis 便携版
; 兼容 Windows Server 2008 / 7+
;
; 制作方法（在开发机上）:
;   1. 安装 Python 3.10+、Node.js 18+
;   2. 安装 Inno Setup 6 (https://jrsoftware.org/isdl.php)
;   3. 双击 build-installer.bat，一键生成
;   4. 输出: Output\SmartTriage-Setup-3.0.0.exe

#define MyAppName "Smart Triage"
#define MyAppVersion "3.0.0"
#define MyAppPublisher "SmartTriage Team"
#define MyAppURL "http://localhost:7017"
#define MyOutputDir "Output"

[Setup]
AppId={{B8A3C4D5-E6F7-8901-2345-6789ABCDEF01}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={pf}\SmartTriage
DefaultGroupName=Smart Triage
AllowNoIcons=yes
OutputDir={#MyOutputDir}
OutputBaseFilename=SmartTriage-Setup-{#MyAppVersion}
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin
DisableProgramGroupPage=yes
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=0,5.2        ; Windows Server 2003 R2 / 2008+
DisableWelcomePage=no

[Languages]
Name: "chinesesimplified"; MessagesFile: "compiler:Languages\ChineseSimplified.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Code]
var
  PortPage: TInputQueryWizardPage;
  DbPage: TInputQueryWizardPage;
  ServicePage: TInputOptionWizardPage;

procedure InitializeWizard;
begin
  // ── 端口 ──
  PortPage := CreateInputQueryPage(
    wpSelectDir, '端口配置', '请设置服务端口',
    '建议 7017，避免与现有系统冲突。可在 settings.env 中修改。');
  PortPage.Add('服务端口:', False);
  PortPage.Values[0] := '7017';

  // ── 数据库 ──
  DbPage := CreateInputQueryPage(
    PortPage.ID, '数据库配置', '请设置 PostgreSQL 连接',
    '安装程序会自动检测已有 PostgreSQL。'#13#10 +
    '如果未安装，将使用内置的便携版 PostgreSQL（自动部署，无需手动安装）。');
  DbPage.Add('主机地址:', False);
  DbPage.Values[0] := '127.0.0.1';
  DbPage.Add('端口:', False);
  DbPage.Values[1] := '5432';
  DbPage.Add('数据库名:', False);
  DbPage.Values[2] := 'smarttriage_new';
  DbPage.Add('用户名:', False);
  DbPage.Values[3] := 'postgres';
  DbPage.Add('密码:', True);
  DbPage.Values[4] := 'postgres';

  // ── 启动方式 ──
  ServicePage := CreateInputOptionPage(
    DbPage.ID, '启动方式', '选择服务启动方式',
    'Windows 服务模式：开机自启，后台静默运行（推荐）。',
    False, False);
  ServicePage.Add('注册为 Windows 服务（开机自启，后台运行）');
  ServicePage.Add('手动启动（托盘模式）');
  ServicePage.Values[0] := True;
end;

function GetAppDir(Param: string): string;
begin
  Result := ExpandConstant('{app}');
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  EnvPath, EnvLines, DbUrl, Port: string;
  ResultCode: Integer;
begin
  if CurStep = ssPostInstall then
  begin
    Port := PortPage.Values[0];
    DbUrl := Format('postgresql+asyncpg://%s:%s@%s:%s/%s',
      [DbPage.Values[3], DbPage.Values[4],
       DbPage.Values[0], DbPage.Values[1], DbPage.Values[2]]);

    // ── 生成 settings.env ──
    EnvPath := ExpandConstant('{app}\settings.env');
    EnvLines :=
      'PORT=' + Port + #13#10 +
      'DATABASE_URL=' + DbUrl + #13#10 +
      'REDIS_HOST=localhost' + #13#10 +
      'REDIS_PORT=6379' + #13#10 +
      'REDIS_DB=1' + #13#10 +
      'JWT_SECRET=smarttriage_' +  + #13#10 +
      'LOG_LEVEL=info' + #13#10;
    SaveStringToFile(EnvPath, EnvLines, False);

    // ── 部署 PostgreSQL（便携版） ──
    // 检测：如果已有 PostgreSQL 或用户使用远程数据库，跳过部署
    if (DbPage.Values[0] = '127.0.0.1') or (DbPage.Values[0] = 'localhost') then
    begin
      Exec(ExpandConstant('{app}\install-pgsql.bat'),
        ExpandConstant('"{app}"'),
        '', SW_SHOW, ewWaitUntilTerminated, ResultCode);
    end;

    // ── 注册 Windows 服务 ──
    if ServicePage.Values[0] then
    begin
      if FileExists(ExpandConstant('{app}\nssm.exe')) then
      begin
        Exec(ExpandConstant('{app}\nssm.exe'),
          Format('install SmartTriage "%s"', [ExpandConstant('{app}\SmartTriage.exe')]),
          '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
        Exec(ExpandConstant('{app}\nssm.exe'),
          'set SmartTriage AppParameters --service',
          '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
        Exec(ExpandConstant('{app}\nssm.exe'),
          'set SmartTriage Start SERVICE_AUTO_START',
          '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
        Exec(ExpandConstant('{app}\nssm.exe'),
          'start SmartTriage',
          '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      end
      else
      begin
        Exec('sc.exe',
          Format('create SmartTriage binPath="%s --service" start=auto DisplayName="Smart Triage"',
            [ExpandConstant('{app}\SmartTriage.exe')]),
          '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
        Exec('sc.exe', 'start SmartTriage', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      end;
    end
    else
    begin
      // 托盘模式：添加开机启动
      RegWriteStringValue(
        HKCU, 'Software\Microsoft\Windows\CurrentVersion\Run',
        'SmartTriage', ExpandConstant('{app}\SmartTriage.exe'));
    end;
  end;
end;

// ── 卸载 ──
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  ResultCode: Integer;
begin
  if CurUninstallStep = usUninstall then
  begin
    // 停止并移除 PostgreSQL 服务（便携版）
    Exec(ExpandConstant('{app}\pgsql\bin\pg_ctl.exe'),
      'stop -D "' + ExpandConstant('{app}\pgsql\data') + '" -m fast',
      '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'delete SmartTriage_PostgreSQL', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);

    // 停止并移除 Smart Triage 服务
    Exec(ExpandConstant('{app}\nssm.exe'), 'stop SmartTriage', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec(ExpandConstant('{app}\nssm.exe'), 'remove SmartTriage confirm', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'delete SmartTriage', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);

    // 移除开机启动
    RegDeleteValue(HKCU, 'Software\Microsoft\Windows\CurrentVersion\Run', 'SmartTriage');
  end;
end;

[Files]
; 主程序（PyInstaller 打包）
Source: "SmartTriage.exe"; DestDir: "{app}"; Flags: ignoreversion
; PostgreSQL 便携版（可选——CI 中未成功下载则跳过）
#if FileExists("pgsql\bin\initdb.exe")
Source: "pgsql\*"; DestDir: "{app}\pgsql"; Flags: ignoreversion recursesubdirs createallsubdirs
#endif
; Redis 便携版（可选）
#if FileExists("redis\redis-server.exe")
Source: "redis\*"; DestDir: "{app}\redis"; Flags: ignoreversion recursesubdirs createallsubdirs
#endif
; Windows 服务管理工具（可选）
#if FileExists("nssm.exe")
Source: "nssm.exe"; DestDir: "{app}"; Flags: ignoreversion
#endif
; PostgreSQL 部署脚本
Source: "install-pgsql.bat"; DestDir: "{app}"; Flags: ignoreversion
; 使用说明
Source: "README.txt"; DestDir: "{app}"; Flags: isreadme

[Icons]
Name: "{commondesktop}\Smart Triage 管理平台"; Filename: "{app}\SmartTriage.exe"; WorkingDir: "{app}"; Tasks: desktopicon
Name: "{group}\Smart Triage 管理平台"; Filename: "{app}\SmartTriage.exe"; WorkingDir: "{app}"
Name: "{group}\配置文件 (settings.env)"; Filename: "{app}\settings.env"
Name: "{group}\PostgreSQL 数据目录"; Filename: "{app}\pgsql\data"
Name: "{group}\卸载 Smart Triage"; Filename: "{uninstallexe}"

[Tasks]
Name: "desktopicon"; Description: "创建桌面快捷方式"; GroupDescription: "快捷方式:"; Flags: checkedonce

[Run]
Filename: "{app}\SmartTriage.exe"; Description: "启动 Smart Triage"; Flags: postinstall nowait skipifsilent unchecked shellexec

[UninstallRun]
Filename: "nssm.exe"; Parameters: "stop SmartTriage"; Flags: runhidden
Filename: "nssm.exe"; Parameters: "remove SmartTriage confirm"; Flags: runhidden
Filename: "{app}\pgsql\bin\pg_ctl.exe"; Parameters: "stop -D ""{app}\pgsql\data"" -m fast"; Flags: runhidden
Filename: "sc.exe"; Parameters: "delete SmartTriage_PostgreSQL"; Flags: runhidden

[UninstallDelete]
Type: files; Name: "{app}\settings.env"
Type: dirifempty; Name: "{app}"