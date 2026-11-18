"""Build without Gradle downloads. Requires Python 3, JDK 17+ and Android SDK."""
import argparse,pathlib,subprocess,zipfile,os
p=argparse.ArgumentParser();p.add_argument('--sdk',required=True);p.add_argument('--jdk',required=True);p.add_argument('--platform',default='android-35');p.add_argument('--build-tools',default='36.0.0');a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];out=root/'build-offline';out.mkdir(exist_ok=True);sdk=pathlib.Path(a.sdk);jdk=pathlib.Path(a.jdk);bt=sdk/'build-tools'/a.build_tools;android=sdk/'platforms'/a.platform/'android.jar';main=root/'app/src/main';classes=out/'classes';classes.mkdir(exist_ok=True)
def run(*args):
 print('RUN',str(args[0]),flush=True);subprocess.run([str(x) for x in args],check=True)
run(bt/'aapt2.exe','link','-I',android,'--manifest',main/'AndroidManifest.xml','-o',out/'base.apk')
run(jdk/'bin/javac.exe','-encoding','UTF-8','-source','8','-target','8','-classpath',android,'-d',classes,*list((main/'java').rglob('*.java')))
run(jdk/'bin/java.exe','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--lib',android,'--min-api','26','--output',out,*list(classes.rglob('*.class')))
with zipfile.ZipFile(out/'base.apk') as src,zipfile.ZipFile(out/'unsigned.apk','w',zipfile.ZIP_DEFLATED) as dst:
 for n in src.namelist():dst.writestr(n,src.read(n))
 dst.write(out/'classes.dex','classes.dex')
run(bt/'zipalign.exe','-f','4',out/'unsigned.apk',out/'aligned.apk')
key=out/'debug.keystore'
if not key.exists():run(jdk/'bin/keytool.exe','-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','androiddebugkey','-dname','CN=Android Debug,O=Android,C=US','-keyalg','RSA','-validity','10000')
run(jdk/'bin/java.exe','-jar',bt/'lib/apksigner.jar','sign','--ks',key,'--ks-pass','pass:android','--out',out/'HealthTrack.apk',out/'aligned.apk')
run(jdk/'bin/java.exe','-jar',bt/'lib/apksigner.jar','verify','--verbose',out/'HealthTrack.apk')
print('APK:',out/'HealthTrack.apk')
