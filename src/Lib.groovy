class Lib {
def stageDecorator(String name, Closure c){
    def r
    try {
        r = c()
    } catch (hudson.AbortException er) {
        println('result')
        println(er)
        println(r)
         throw er
    } catch (Exception er1) {
        println(er1)
        throw er1
    }
    return r

}
}