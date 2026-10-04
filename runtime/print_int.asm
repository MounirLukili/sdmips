# PrintInt : affiche l entier $a0 puis un retour a la ligne
entryPrintInt:
	li $v0, 1
	syscall
	li $a0, 10
	li $v0, 11
	syscall
exitPrintInt:
	jr $ra
