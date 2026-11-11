package br.com.sungabranca.data

object CatalogSeed {

    private fun ex(name: String, muscleGroup: String, equipment: String) =
        ExerciseEntity(name = name, muscleGroup = muscleGroup, equipment = equipment)

    val exercises: List<ExerciseEntity> = listOf(
        ex("Supino reto com barra", "Peito", "Barra"),
        ex("Supino inclinado com halteres", "Peito", "Halteres"),
        ex("Flexão de braço", "Peito", "Peso corporal"),
        ex("Puxada na frente", "Costas", "Máquina"),
        ex("Remada curvada com barra", "Costas", "Barra"),
        ex("Barra fixa", "Costas", "Peso corporal"),
        ex("Desenvolvimento com halteres", "Ombros", "Halteres"),
        ex("Elevação lateral", "Ombros", "Halteres"),
        ex("Rosca direta", "Bíceps", "Barra"),
        ex("Tríceps na polia", "Tríceps", "Polia"),
        ex("Agachamento livre", "Pernas", "Barra"),
        ex("Leg press", "Pernas", "Máquina"),
        ex("Cadeira extensora", "Quadríceps", "Máquina"),
        ex("Mesa flexora", "Posterior de coxa", "Máquina"),
        ex("Levantamento terra romeno", "Posterior de coxa", "Barra"),
        ex("Panturrilha em pé", "Panturrilha", "Máquina"),
        ex("Abdominal prancha", "Abdômen", "Peso corporal")
    )
}