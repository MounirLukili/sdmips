#!/usr/bin/env bash
# ---------------------------------------------------------------------------
#  Tests du compilateur (à lancer depuis la racine du dépôt : make test)
#
#  tests/ok/NOM.sdm       programme valide ; sa sortie à l'exécution doit être
#                         égale à NOM.attendu (entrée clavier : NOM.entree)
#  tests/erreurs/NOM.sdm  programme invalide ; la compilation doit échouer avec
#                         le message indiqué en première ligne : // ERREUR : ...
#
#  Simulateur : SPIM par défaut, ou MARS si MARS_JAR=/chemin/Mars4_5.jar.
# ---------------------------------------------------------------------------
set -u

JAR=build/sdmips.jar
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

reussis=0
echecs=0
ok()    { echo "  [OK]    $1"; reussis=$((reussis + 1)); }
echec() { echo "  [ÉCHEC] $1"; echecs=$((echecs + 1)); }

# Exécute un fichier assembleur et affiche uniquement la sortie du programme.
executer() {
    local asm=$1 entree=$2
    if [ -n "${MARS_JAR:-}" ]; then
        java -jar "$MARS_JAR" nc "$asm" < "$entree" 2>&1
    else
        # SPIM affiche un en-tête qui se termine par la ligne "Loaded: ...exceptions.s".
        spim -file "$asm" < "$entree" 2>&1 | sed '1,/^Loaded:/d'
    fi
}

# Supprime les lignes vides en fin de sortie.
normaliser() {
    sed -e :a -e '/^\n*$/{$d;N;ba' -e '}'
}

if [ -n "${MARS_JAR:-}" ]; then
    echo "Simulateur : MARS ($MARS_JAR)"
elif command -v spim > /dev/null; then
    echo "Simulateur : $(command -v spim)"
else
    echo "SPIM est introuvable (ou définir MARS_JAR=/chemin/Mars4_5.jar)." >&2
    exit 1
fi

echo "Programmes valides (tests/ok)"
for source in tests/ok/*.sdm; do
    nom=$(basename "$source" .sdm)
    attendu=tests/ok/$nom.attendu
    entree=tests/ok/$nom.entree
    [ -f "$entree" ] || entree=/dev/null

    if ! java -jar "$JAR" -o "$TMP/$nom.asm" "$source" > "$TMP/compil.txt" 2>&1; then
        echec "$nom : la compilation a échoué"
        sed 's/^/          /' "$TMP/compil.txt"
        continue
    fi
    timeout 20 bash -c "$(declare -f executer); executer '$TMP/$nom.asm' '$entree'" \
        | normaliser > "$TMP/$nom.sortie"
    if diff -u <(normaliser < "$attendu") "$TMP/$nom.sortie" > "$TMP/diff.txt"; then
        ok "$nom"
    else
        echec "$nom : sortie différente de $attendu"
        sed 's/^/          /' "$TMP/diff.txt"
    fi
done

echo "Programmes invalides (tests/erreurs)"
for source in tests/erreurs/*.sdm; do
    nom=$(basename "$source" .sdm)
    message=$(head -n 1 "$source" | sed -n 's|^// *ERREUR *: *||p')
    java -jar "$JAR" -o "$TMP/$nom.asm" "$source" > "$TMP/compil.txt" 2>&1
    code=$?
    if [ "$code" -eq 0 ]; then
        echec "$nom : la compilation aurait dû échouer"
    elif ! grep -qF -- "$message" "$TMP/compil.txt"; then
        echec "$nom : message « $message » absent"
        sed 's/^/          /' "$TMP/compil.txt"
    else
        ok "$nom"
    fi
done

echo
echo "$reussis test(s) réussi(s), $echecs échec(s)"
[ "$echecs" -eq 0 ]
