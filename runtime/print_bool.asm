# PrintBool : affiche true ou false selon $a0, puis un retour a la ligne
	.data
chaineTrue:
	.asciiz "true\n"
chaineFalse:
	.asciiz "false\n"
	.text
entryPrintBool:
	la $t0, chaineTrue
	bnez $a0, printBoolEcrire
	la $t0, chaineFalse
printBoolEcrire:
	move $a0, $t0
	li $v0, 4
	syscall
exitPrintBool:
	jr $ra
