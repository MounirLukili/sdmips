# ---------------------------------------------------------------------------
#  sdmips — compilateur SDM -> MIPS
# ---------------------------------------------------------------------------
#  make          télécharge ANTLR si besoin, génère l'analyseur, compile,
#                et produit build/sdmips.jar (autonome)
#  make test     lance les tests (nécessite le simulateur SPIM)
#  make clean    supprime build/
#
#  Exemple :     ./sdmips tests/ok/factorielle.sdm && spim -file tests/ok/factorielle.asm
# ---------------------------------------------------------------------------

ANTLR_VERSION = 4.13.2
ANTLR_JAR     = lib/antlr-$(ANTLR_VERSION)-complete.jar
ANTLR_URL     = https://www.antlr.org/download/antlr-$(ANTLR_VERSION)-complete.jar

# Outil ANTLR (génération de l'analyseur) et bibliothèque d'exécution ANTLR
# (embarquée dans le jar final). Les deux viennent du jar "complete" par défaut.
ANTLR_TOOL        = java -jar $(ANTLR_JAR)
ANTLR_RUNTIME_JAR = $(ANTLR_JAR)

JAVAC      = javac
JAVACFLAGS = -encoding UTF-8 --release 17 -Xlint:all

BUILD     = build
GENERATED = $(BUILD)/generated/parser
CLASSES   = $(BUILD)/classes
JAR       = $(BUILD)/sdmips.jar

GRAMMAR  = grammar/Sdm.g4
SOURCES := $(shell find src -name '*.java')
RUNTIME := $(wildcard runtime/*.asm)

.PHONY: all test clean

all: $(JAR)

# Téléchargement d'ANTLR (une seule fois)
$(ANTLR_JAR):
	@mkdir -p $(dir $@)
	curl -fL --retry 3 -o $@ $(ANTLR_URL)

# Génération de l'analyseur lexical et syntaxique
$(GENERATED)/.stamp: $(GRAMMAR) | $(ANTLR_RUNTIME_JAR)
	@rm -rf $(GENERATED) && mkdir -p $(GENERATED)
	$(ANTLR_TOOL) -visitor -no-listener -package parser -Xexact-output-dir -o $(GENERATED) $(GRAMMAR)
	@touch $@

# Compilation et assemblage d'un jar autonome (ANTLR runtime inclus)
$(JAR): $(GENERATED)/.stamp $(SOURCES) $(RUNTIME)
	@rm -rf $(CLASSES) && mkdir -p $(CLASSES)/runtime
	$(JAVAC) -encoding UTF-8 --release 17 -nowarn -cp $(ANTLR_RUNTIME_JAR) -d $(CLASSES) $(GENERATED)/*.java
	$(JAVAC) $(JAVACFLAGS) -cp $(ANTLR_RUNTIME_JAR):$(CLASSES) -d $(CLASSES) $(SOURCES)
	cp $(RUNTIME) $(CLASSES)/runtime/
	cd $(CLASSES) && jar xf $(abspath $(ANTLR_RUNTIME_JAR)) org/antlr/v4/runtime
	jar --create --file $@ --main-class main.Main -C $(CLASSES) .
	@echo "-> $@"

test: $(JAR)
	./tests/run_tests.sh

clean:
	rm -rf $(BUILD)
