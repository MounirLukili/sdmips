# ReadInt : lit un entier au clavier, resultat dans $v0
entryReadInt:
	li $v0, 5
	syscall
exitReadInt:
	jr $ra
