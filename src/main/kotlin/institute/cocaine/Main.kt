package institute.cocaine

suspend fun main(args: Array<String>) {
    if (args.isEmpty())
        return
    println("Starting new bot.")
    Bot(args[0])
}