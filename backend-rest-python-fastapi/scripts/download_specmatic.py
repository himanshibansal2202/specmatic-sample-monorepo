from pathlib import Path
from urllib.request import urlretrieve


VERSION = "1.19.1"
ARTIFACT = "io.specmatic.enterprise:executable-all"
JAR = Path("tools") / f"specmatic-enterprise-executable-all-{VERSION}.jar"
URL = f"https://repo1.maven.org/maven2/io/specmatic/enterprise/executable-all/{VERSION}/executable-all-{VERSION}.jar"


def main() -> None:
    JAR.parent.mkdir(parents=True, exist_ok=True)
    if JAR.exists():
        print(JAR)
        return
    print(f"Downloading {ARTIFACT}:{VERSION} from Maven Central")
    urlretrieve(URL, JAR)
    print(JAR)


if __name__ == "__main__":
    main()
