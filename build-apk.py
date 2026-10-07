#!/usr/bin/env python3
"""Build a signed, installable APK on Linux with Java 17 and the official SDK tools.

No Gradle daemon or extra Android libraries are needed. Downloaded tools are cached
outside the source tree. A local signing key is created on first run; keep it to
sign future updates for the same installed app.
"""
from pathlib import Path
import argparse
import os
import shutil
import subprocess
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parent
parser = argparse.ArgumentParser()
parser.add_argument('--tools', type=Path, default=ROOT.parent / 'android-tools')
parser.add_argument('--output', type=Path, default=ROOT / 'build' / 'Sexta-Feira.apk')
parser.add_argument('--keystore', type=Path, default=os.environ.get('SEXTA_KEYSTORE_PATH') or None)
parser.add_argument('--test', action='store_true', help='Run the local command checks before packaging')
args = parser.parse_args()
if args.keystore and (not args.keystore.is_file() or not os.environ.get('SEXTA_STORE_PASSWORD')):
    parser.error('A supplied keystore must exist and SEXTA_STORE_PASSWORD must be set.')
tools = args.tools.resolve()
tools.mkdir(parents=True, exist_ok=True)
build = ROOT / 'build'
build.mkdir(exist_ok=True)
output = args.output.resolve()
output.parent.mkdir(parents=True, exist_ok=True)

def download(name, url):
    path = tools / name
    if not path.exists():
        print('Downloading', name, flush=True)
        temporary = path.with_suffix('.part')
        try:
            with urllib.request.urlopen(url, timeout=90) as r, temporary.open('wb') as f:
                shutil.copyfileobj(r, f)
            temporary.replace(path)
        finally:
            temporary.unlink(missing_ok=True)
    return path

sdk = tools / 'android-15'
if not (sdk / 'aapt2').exists():
    archive = download('build-tools.zip', 'https://dl.google.com/android/repository/build-tools_r35.0.1_linux.zip')
    with zipfile.ZipFile(archive) as z:
        z.extractall(tools)
for name in ['aapt2','apksigner','zipalign','d8','dexdump']:
    (sdk/name).chmod(0o755)
android = tools / 'android.jar'
if not android.exists():
    archive = download('platform.zip', 'https://dl.google.com/android/repository/platform-35_r02.zip')
    with zipfile.ZipFile(archive) as z:
        entry = next(n for n in z.namelist() if n.endswith('/android.jar'))
        android.write_bytes(z.read(entry))
ecj = download('ecj.jar', 'https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.37.0/ecj-3.37.0.jar')

def run(*cmd):
    subprocess.run([str(x) for x in cmd], cwd=ROOT, check=True)

source = ROOT / 'app' / 'src' / 'main'
manifest = ET.parse(source/'AndroidManifest.xml')
manifest.getroot().set('package','com.gustavo.sextafeira')
ET.register_namespace('android','http://schemas.android.com/apk/res/android')
manifest.write(build/'AndroidManifest.xml', encoding='utf-8', xml_declaration=True)
compiled = build/'compiled-resources.zip'
generated = build/'generated'
generated.mkdir(exist_ok=True)
run(sdk/'aapt2','compile','--dir',source/'res','-o',compiled)
run(sdk/'aapt2','link','-o',build/'base.apk','-I',android,'--manifest',build/'AndroidManifest.xml',
    '--min-sdk-version','26','--target-sdk-version','35','--version-code','1','--version-name','1.0',
    '-A',source/'assets','--java',generated,compiled)
classes = build/'classes'
if classes.exists(): shutil.rmtree(classes)
classes.mkdir()
java_sources = sorted((source/'java').rglob('*.java')) + sorted(generated.rglob('*.java'))
bootclasspath=str(android)+os.pathsep+str(sdk/'core-lambda-stubs.jar')
run('java','-jar',ecj,'-source','1.8','-target','1.8','-bootclasspath',bootclasspath,
    '-encoding','UTF-8','-warn:none','-d',classes,*java_sources)
if args.test:
    test_classes=build/'test-classes'
    test_classes.mkdir(exist_ok=True)
    run('java','-jar',ecj,'-source','1.8','-target','1.8','-bootclasspath',bootclasspath,
        '-cp',classes,'-encoding','UTF-8','-warn:none','-d',test_classes,
        ROOT/'tests'/'CommandParserCheck.java')
    run('java','-cp',str(classes)+os.pathsep+str(test_classes),'CommandParserCheck')
with zipfile.ZipFile(build/'classes.jar','w',zipfile.ZIP_DEFLATED) as z:
    for path in sorted(classes.rglob('*.class')): z.write(path,path.relative_to(classes))
dex = build/'dex'
dex.mkdir(exist_ok=True)
run('java','-cp',sdk/'lib'/'d8.jar','com.android.tools.r8.D8','--lib',android,
    '--min-api','26','--output',dex,build/'classes.jar')
unsigned = build/'unsigned.apk'
shutil.copyfile(build/'base.apk',unsigned)
with zipfile.ZipFile(unsigned,'a',zipfile.ZIP_DEFLATED) as z:
    for path in sorted(dex.glob('*.dex')): z.write(path,path.name)
aligned = build/'aligned.apk'
run(sdk/'zipalign','-f','-p','4',unsigned,aligned)
key = args.keystore.resolve() if args.keystore else tools/'sexta-feira-local.keystore'
key_password=os.environ['SEXTA_STORE_PASSWORD'] if args.keystore else 'sexta-feira-local-build'
key_alias=os.environ.get('SEXTA_KEY_ALIAS','sexta-feira') if args.keystore else 'sexta-feira'
if not args.keystore and not key.exists():
    run('keytool','-genkeypair','-keystore',key,'-storepass',key_password,'-keypass',key_password,
        '-alias','sexta-feira','-keyalg','RSA','-keysize','2048','-validity','10000',
        '-dname','CN=Sexta Feira, OU=Personal App, O=Gustavo, C=BR')
    key.chmod(0o600)
# The local personal-build password is passed by environment instead of command arguments.
signing_env = dict(os.environ, SEXTA_LOCAL_STORE_PASS=key_password,
                  SEXTA_LOCAL_KEY_PASS=os.environ.get('SEXTA_KEY_PASSWORD',key_password) if args.keystore else key_password)
subprocess.run([str(sdk/'apksigner'),'sign','--ks',str(key),'--ks-key-alias',key_alias,
    '--ks-pass','env:SEXTA_LOCAL_STORE_PASS','--key-pass','env:SEXTA_LOCAL_KEY_PASS',
    '--out',str(output),str(aligned)],env=signing_env,check=True)
run(sdk/'apksigner','verify','--verbose',output)
run(sdk/'zipalign','-c','4',output)
print('APK ready:',output)
