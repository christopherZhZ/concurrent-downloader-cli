import click

class Printer:
    def new_line(self):
        click.secho('')

    def success(self, text):
        click.secho(str(text), fg='bright_green')

    def fail(self, text):
        click.secho(str(text), fg='bright_red')

    def err(self, text):
        click.secho(str(text), fg='red')

    def warn(self, text):
        click.secho(str(text), fg='bright_yellow')

    def general(self, text):
        click.secho(str(text), fg='bright_white')

printer = Printer()
