#!/usr/bin/env python3
import os

# Какие файлы считаем исходниками/конфигами
SOURCE_EXTENSIONS = {
    ".java", ".kt", ".kts", ".groovy", ".scala",
    ".xml", ".properties", ".yml", ".yaml", ".json",
    ".gradle", ".gitignore", ".md"
}

# Можно добавить/убрать каталоги, которые нужно пропускать
EXCLUDED_DIRS = {
    ".git", ".idea", ".gradle",
    "build", "out", "target",
    "node_modules", "reports"
}

OUTPUT_FILE = "project_sources_concat.txt"


def should_exclude_dir(dirname: str) -> bool:
    return dirname in EXCLUDED_DIRS


def is_source_file(filename: str) -> bool:
    _, ext = os.path.splitext(filename)
    return ext in SOURCE_EXTENSIONS


def main():
    project_root = os.path.abspath(os.getcwd())
    output_path = os.path.join(project_root, OUTPUT_FILE)

    with open(output_path, "w", encoding="utf-8") as out:
        for root, dirs, files in os.walk(project_root):
            # Фильтрация ненужных директорий
            dirs[:] = [d for d in dirs if not should_exclude_dir(d)]

            for fname in files:
                if not is_source_file(fname):
                    continue

                full_path = os.path.join(root, fname)
                # Относительный путь от корня проекта
                rel_path = os.path.relpath(full_path, project_root)

                out.write("=" * 80 + "\n")
                out.write(f"FILE: {rel_path}\n")
                out.write("=" * 80 + "\n\n")

                try:
                    with open(full_path, "r", encoding="utf-8") as f:
                        out.write(f.read())
                except UnicodeDecodeError:
                    # Если файл не в UTF-8 — пропускаем или можно добавить другую логику
                    out.write("[SKIPPED: encoding error]\n")

                out.write("\n\n")  # Отступ между файлами

    print(f"Готово. Файл собран в: {output_path}")


if __name__ == "__main__":
    main()