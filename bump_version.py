import re
import sys

def bump_version():
    with open('app/build.gradle.kts', 'r', encoding='utf-8') as f:
        content = f.read()

    # Find versionCode
    vc_match = re.search(r'versionCode\s*=\s*(\d+)', content)
    if not vc_match:
        print("Could not find versionCode")
        sys.exit(1)
    
    current_vc = int(vc_match.group(1))
    new_vc = current_vc + 1

    # Find versionName
    vn_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
    if not vn_match:
        print("Could not find versionName")
        sys.exit(1)

    current_vn = vn_match.group(1)
    parts = current_vn.split('.')
    if len(parts) > 0 and parts[-1].isdigit():
        parts[-1] = str(int(parts[-1]) + 1)
        new_vn = ".".join(parts)
    else:
        new_vn = current_vn + ".1"

    # Replace
    content = re.sub(r'versionCode\s*=\s*\d+', f'versionCode = {new_vc}', content)
    content = re.sub(r'versionName\s*=\s*"[^"]+"', f'versionName = "{new_vn}"', content)

    with open('app/build.gradle.kts', 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"Bumped to versionCode {new_vc}, versionName {new_vn}")

if __name__ == "__main__":
    bump_version()
